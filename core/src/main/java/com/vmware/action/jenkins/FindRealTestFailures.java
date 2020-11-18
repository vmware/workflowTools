package com.vmware.action.jenkins;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.vmware.BuildResult;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.HomePage;
import com.vmware.jenkins.domain.JobDetails;
import com.vmware.jenkins.domain.TestNGResults;
import com.vmware.jenkins.domain.ViewDetails;
import com.vmware.util.ClasspathResource;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.collection.BlockingExecutorService;

import org.slf4j.LoggerFactory;

import static com.vmware.util.StringUtils.pluralize;

@ActionDescription("Finds real test failures for a Jenkins View. Real failures are tests that are continuously failing as opposed to one offs.")
public class FindRealTestFailures extends BaseAction {

    private BlockingExecutorService<Jenkins> jenkinsExecutor;
    private String generationDate;

    public FindRealTestFailures(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("jenkinsView", "destinationFile");
    }

    @Override
    public void process() {
        this.jenkinsExecutor = new BlockingExecutorService<>(6, () -> {
            LoggerFactory.getLogger(FindRealTestFailures.class).debug("Creating new service");
            return serviceLocator.newJenkins();
        });
        long startTime = System.currentTimeMillis();

        HomePage homePage = serviceLocator.getJenkins().getHomePage();

        List<HomePage.View> matchingViews = Arrays.stream(homePage.views).filter(view -> view.matches(jenkinsConfig.jenkinsView)).collect(Collectors.toList());

        if (matchingViews.isEmpty()) {
            exitDueToFailureCheck("No views found for name " + jenkinsConfig.jenkinsView);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss aa zzz yyyy");
        generationDate = formatter.format(new Date());

        File destinationFile = new File(fileSystemConfig.destinationFile);
        if (matchingViews.size() > 1) {
            log.info("Matched {} views to view name {}", matchingViews.size(), jenkinsConfig.jenkinsView);
            log.info("View names: {}", matchingViews.stream().map(view -> view.name).collect(Collectors.joining(", ")));
            if (!destinationFile.exists()) {
                failIfTrue(!destinationFile.mkdir(), "Failed to create directory " + destinationFile.getAbsolutePath());
            }
        }

        log.info("Checking last {} builds for tests that are failing in the latest build and have failed in previous builds as well",
                jenkinsConfig.maxJenkinsBuildsToCheck);

        if (matchingViews.size() > 1) {
            failIfTrue(destinationFile.exists() && destinationFile.isFile(),
                    destinationFile.getAbsolutePath() + " is a file. Destination file needs to specify a directory");
        }
        matchingViews.forEach(this::saveFailuresPageForView);

        if (matchingViews.size() > 1) {
            String viewListing = createViewListingHtml(matchingViews);
            fileSystemConfig.destinationFile = fileSystemConfig.destinationFile + File.separator + "index.html";
            log.info("Saving view listing to {}", fileSystemConfig.destinationFile);
            IOUtils.write(new File(fileSystemConfig.destinationFile), viewListing);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        log.info("Took {} seconds", TimeUnit.MILLISECONDS.toSeconds(elapsedTime));
    }

    private String createViewListingHtml(List<HomePage.View> matchingViews) {
        String viewListingHtml = matchingViews.stream().map(HomePage.View::listHtmlFragment).collect(Collectors.joining("\n"));
        String viewListingPage = new ClasspathResource("/testFailuresTemplate/viewListing.html", this.getClass()).getText();
        viewListingPage = viewListingPage.replace("#viewPattern", jenkinsConfig.jenkinsView);
        viewListingPage = viewListingPage.replace("#date", generationDate);
        return viewListingPage.replace("#body", viewListingHtml);
    }

    private void saveFailuresPageForView(HomePage.View view) {
        final Map<JobDetails, List<TestNGResults.TestMethod>> failingTestMethods;
        try {
            failingTestMethods = findAllRealFailingTests(view.url);
        } catch (Exception e) {
            log.error("Failed to create page for view " + view.name, e);
            view.failingTestsGenerationException = e;
            return;
        }

        view.failingTestsCount = failingTestMethods.values().stream().mapToInt(List::size).sum();
        log.info("{} failing tests found for view {}", view.failingTestsCount, view.name);
        if (failingTestMethods.isEmpty()) {
            return;
        }

        final StringBuilder jobFragments = new StringBuilder("");
        final AtomicInteger counter = new AtomicInteger();
        failingTestMethods.keySet().stream().sorted(Comparator.comparing(jobDetails -> jobDetails.name)).forEach(key -> {
            List<TestNGResults.TestMethod> failingTests = failingTestMethods.get(key);
            String jobFragment = createJobFragment(counter.getAndIncrement(), key, failingTests);
            if (jobFragments.length() > 0) {
                jobFragments.append("\n");
            }
            jobFragments.append(jobFragment);
        });

        String failuresPage = new ClasspathResource("/testFailuresTemplate/testFailuresWebPage.html", this.getClass()).getText();
        String filledInFailures = failuresPage.replace("#body", jobFragments.toString());
        filledInFailures = filledInFailures.replace("#date", generationDate);
        filledInFailures = filledInFailures.replace("#viewName", view.viewNameWithFailureCount());
        log.trace("Test Failures for view {}:\n{}", view.name, filledInFailures);

        File destinationFile = new File(fileSystemConfig.destinationFile);
        if (destinationFile.exists() && destinationFile.isDirectory()) {
            File failurePageFile = new File(fileSystemConfig.destinationFile + File.separator + view.htmlFileName());
            log.info("Saving test failures for {} to {}", view.name, failurePageFile);
            IOUtils.write(failurePageFile, filledInFailures);
        } else {
            log.info("Saving test failures for {} to {}", view.name, destinationFile);
            IOUtils.write(destinationFile, filledInFailures);
        }
    }

    private Map<JobDetails, List<TestNGResults.TestMethod>> findAllRealFailingTests(String viewUrl) {
        Jenkins jenkins = serviceLocator.getJenkins();
        ViewDetails viewDetails = jenkins.getFullViewDetails(viewUrl);
        Map<JobDetails, List<TestNGResults.TestMethod>> allFailingTests = new HashMap<>();

        log.info("");
        log.info("Checking {} jobs in {} for test failures", viewDetails.jobs.length, viewDetails.name);

        Arrays.stream(viewDetails.jobs).parallel().forEach(jobDetails -> {
            if (jobDetails.lastCompletedBuild == null) {
                log.info("Skipping job {} as there are no recent completed builds", jobDetails.name);
                return;
            }
            if (jobDetails.lastBuildWasSuccessful()) {
                log.info("Skipping job {} as most recent build was successful", jobDetails.name);
                return;
            }
            if (jobDetails.lastUnstableBuild == null) {
                log.info("Skipping job {} as there are no recent unstable builds", jobDetails.name);
                return;
            }

            if (jobDetails.lastUnstableBuildAge() > 4) {
                log.info("Skipping job {} as last unstable build was {} builds ago", jobDetails.name, jobDetails.lastUnstableBuildAge());
                return;
            }
            try {
                JobDetails fullDetails = jenkinsExecutor.execute(j -> j.getJobDetails(jobDetails.getFullInfoUrl()));
                List<TestNGResults.TestMethod> failingTests = findFailingTestsForJob(fullDetails);
                if (failingTests.isEmpty()) {
                    log.info("No consistently failing tests found for job {}", fullDetails.name);
                    return;
                }
                log.info("Found {} consistently failing tests for job {}", failingTests.size(), fullDetails.name);
                allFailingTests.put(fullDetails, failingTests);
            } catch (Exception e) {
                log.error("Failed to get full job details for {}\n{}", jobDetails.name, StringUtils.exceptionAsString(e));
                throw e;
            }
        });

        return allFailingTests;
    }

    private List<TestNGResults.TestMethod> findFailingTestsForJob(JobDetails fullDetails) {
        if (fullDetails.builds[0].result == BuildResult.SUCCESS) {
            log.info("Most recent build for job {} passed", fullDetails.name);
            return Collections.emptyList();
        }
        List<TestNGResults> usableResults =
                Arrays.stream(fullDetails.builds).parallel().sorted((first, second) -> second.number.compareTo(first.number))
                        .limit(jenkinsConfig.maxJenkinsBuildsToCheck).map(build -> {
                    if (build.result == BuildResult.UNSTABLE) {
                        TestNGResults results = jenkinsExecutor.execute(j -> j.getJobBuildTestResults(build));
                        results.buildResult = build.result;
                        results.jobName = build.fullDisplayName;
                        results.buildNumber = build.number;
                        results.uiUrl = build.getTestReportsUIUrl();
                        return results;
                    } else if (build.result == BuildResult.SUCCESS) {
                        TestNGResults results = new TestNGResults();
                        results.buildResult = build.result;
                        results.jobName = build.fullDisplayName;
                        results.buildNumber = build.number;
                        results.uiUrl = build.getTestReportsUIUrl();
                        return results;
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());

        Map<TestNGResults.TestMethod, List<TestNGResults.TestMethod>> testResults = createTestMethodResultsMap(usableResults);
        return testResults.entrySet().stream().filter(this::isRealFailure).map(this::testMethodWithInfo).collect(Collectors.toList());
    }

    private TestNGResults.TestMethod testMethodWithInfo(Map.Entry<TestNGResults.TestMethod, List<TestNGResults.TestMethod>> entry) {
        TestNGResults.TestMethod testMethod = entry.getKey();
        testMethod.testRuns = new ArrayList<>();
        testMethod.testRuns.addAll(entry.getValue().stream().filter(method -> method.status == TestNGResults.TestResult.FAIL).collect(Collectors.toList()));
        testMethod.testRuns.addAll(entry.getValue().stream().filter(method -> method.status == TestNGResults.TestResult.PASS).collect(Collectors.toList()));
        return entry.getKey();
    }

    private Map<TestNGResults.TestMethod, List<TestNGResults.TestMethod>> createTestMethodResultsMap(List<TestNGResults> usableResults) {
        Map<TestNGResults.TestMethod, List<TestNGResults.TestMethod>> testResults = new HashMap<>();
        usableResults.forEach(testNGResults -> {
            log.debug("Processing build {}", testNGResults.jobName);
            if (testNGResults.buildResult != BuildResult.SUCCESS) {
                List<TestNGResults.TestMethod> testMethods = testNGResults.testMethods();
                testMethods.forEach(testMethod -> {
                    TestNGResults.TestMethod matchingTestMethod =
                            testResults.keySet().stream().filter(key -> key.fullTestNameWithPackage().equals(testMethod.fullTestNameWithPackage()))
                                    .findFirst().orElseGet(() -> {
                                testResults.put(testMethod, new ArrayList<>());
                                return testMethod;
                            });
                    // skipped test
                    if (matchingTestMethod.status == null && testMethod.status == TestNGResults.TestResult.FAIL) {
                        log.debug("Using older test method failure for test {}", testMethod.fullTestName());
                        testResults.put(testMethod, testResults.get(matchingTestMethod));
                        testResults.remove(matchingTestMethod);
                        testResults.get(testMethod).add(testMethod);
                    } else {
                        testResults.get(matchingTestMethod).add(testMethod);
                    }
                });
            } else {
                Set<String> usedUrls = new HashSet<>();
                testResults.forEach((key, value) -> {
                    TestNGResults.TestMethod dummyPassMethod = new TestNGResults.TestMethod(key, TestNGResults.TestResult.PASS, testNGResults.buildNumber);
                    dummyPassMethod.setUrlForTestMethod(testNGResults.uiUrl, usedUrls);
                    usedUrls.add(dummyPassMethod.url);
                    value.add(dummyPassMethod);
                });
            }
        });
        return testResults;
    }

    private boolean isRealFailure(Map.Entry<TestNGResults.TestMethod, List<TestNGResults.TestMethod>> entry) {
        if (entry.getValue().get(0).status == TestNGResults.TestResult.PASS) {
            return false;
        }
        return entry.getValue().stream().filter(value -> value.status == TestNGResults.TestResult.FAIL).count() > 1;
    }

    private String createJobFragment(int jobIndex, JobDetails fullDetails, List<TestNGResults.TestMethod> failingMethods) {
        String jobFragment = new ClasspathResource("/testFailuresTemplate/jobFailures.html", this.getClass()).getText();
        String rowFragment = new ClasspathResource("/testFailuresTemplate/testFailureRows.html", this.getClass()).getText();

        String filledInJobFragment = jobFragment.replace("#url", fullDetails.url);
        filledInJobFragment = filledInJobFragment.replace("#jobName", fullDetails.name);
        filledInJobFragment = filledInJobFragment.replace("#failingTestCount", pluralize(failingMethods.size(), "failing test"));
        filledInJobFragment = filledInJobFragment.replace("#itemId", "item-" + jobIndex);

        StringBuilder rowBuilder = new StringBuilder("");
        int index = 0;
        for (TestNGResults.TestMethod method : failingMethods) {
            String filledInRow = rowFragment.replace("#testName", method.fullTestName());
            filledInRow = filledInRow.replace("#testResultDescription", method.testResultsLinks());
            filledInRow = filledInRow.replace("#itemId", "item-" + jobIndex + "-" + index++);
            if (method.status == null) {
                log.warn("No status found for test method {}", method.fullTestName());
            }
            filledInRow = filledInRow.replace("#url", method.url);
            filledInRow = filledInRow.replace("#exception", method.exception != null ? method.exception.replace("\n", "<br/>") : "");
            rowBuilder.append(filledInRow).append("\n");
        }
        filledInJobFragment = filledInJobFragment.replace("#body", rowBuilder.toString());
        return filledInJobFragment;
    }
}
