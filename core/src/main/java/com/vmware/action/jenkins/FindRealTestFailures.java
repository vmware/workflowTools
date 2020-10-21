package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vmware.BuildResult;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.JobDetails;
import com.vmware.jenkins.domain.TestNGResults;
import com.vmware.jenkins.domain.ViewDetails;
import com.vmware.util.ClasspathResource;
import com.vmware.util.collection.BlockingExecutorService;

import org.slf4j.LoggerFactory;

@ActionDescription("Finds real test failures for a Jenkins View. Real failures are tests that are continuously failing as opposed to one offs.")
public class FindRealTestFailures extends BaseAction {

    private BlockingExecutorService<Jenkins> jenkinsExecutor;

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
        long startTime = new Date().getTime();
        String jobFragments = createJobFragmentsHtml();

        String failuresPage = new ClasspathResource("/testFailuresTemplate/testFailuresWebPage.html", this.getClass()).getText();
        String filledInFailures = failuresPage.replace("#body", jobFragments);
        filledInFailures = filledInFailures.replace("#viewName", jenkinsConfig.jenkinsView);
        log.trace("Test Failures:\n{}", filledInFailures);
        fileSystemConfig.fileData = filledInFailures;
        long elapsedTime = new Date().getTime() - startTime;
        log.info("Took {} seconds", TimeUnit.MILLISECONDS.toSeconds(elapsedTime));
    }

    private String createJobFragmentsHtml() {
        Map<JobDetails, List<TestNGResults.TestMethod>> failingTestMethods = findAllRealFailingTests();
        if (failingTestMethods.isEmpty()) {
            cancelWithMessage("no consistently failing tests found for any job");
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
        return jobFragments.toString();
    }

    private Map<JobDetails, List<TestNGResults.TestMethod>> findAllRealFailingTests() {
        Jenkins jenkins = serviceLocator.getJenkins();
        ViewDetails viewDetails = jenkins.getViewDetails(jenkinsConfig.jenkinsView);
        Map<JobDetails, List<TestNGResults.TestMethod>> allFailingTests = new HashMap<>();

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
                log.error("Failed to get full job details for {}", jobDetails.name);
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
                Arrays.stream(fullDetails.builds).parallel().sorted((first, second) -> second.number.compareTo(first.number)).limit(7).map(build -> {
                    if (build.result == BuildResult.UNSTABLE) {
                        TestNGResults results = jenkinsExecutor.execute(j -> j.getJobBuildTestResults(build));
                        results.uiUrl = build.getTestReportsUIUrl();
                        return results;
                    } else if (build.result == BuildResult.SUCCESS) {
                        return new TestNGResults();
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());

        Map<TestNGResults.TestMethod, List<TestNGResults.TestResult>> testResults = createTestMethodResultsMap(usableResults);

        return testResults.entrySet().stream().filter(this::isRealFailure).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private Map<TestNGResults.TestMethod, List<TestNGResults.TestResult>> createTestMethodResultsMap(List<TestNGResults> usableResults) {
        final AtomicInteger successCount = new AtomicInteger(0);
        Map<TestNGResults.TestMethod, List<TestNGResults.TestResult>> testResults = new HashMap<>();
        usableResults.forEach(testNGResults -> {
            if (testNGResults.failCount > 0) {
                List<TestNGResults.TestMethod> testMethods = testNGResults.testMethods();
                testMethods.forEach(testMethod -> {
                    TestNGResults.TestMethod matchingTestMethod =
                            testResults.keySet().stream().filter(key -> key.fullTestName().equals(testMethod.fullTestName())).findFirst().orElseGet(() -> {
                                testResults.put(testMethod, new ArrayList<>());
                                return testMethod;
                            });
                    IntStream.range(0, successCount.get()).forEach(index -> testResults.get(matchingTestMethod).add(TestNGResults.TestResult.PASS));
                    // skipped test
                    if (matchingTestMethod.status == null && testMethod.status == TestNGResults.TestResult.FAIL) {
                        log.debug("Using older test method failure for test {}", testMethod.fullTestName());
                        testResults.put(testMethod, testResults.get(matchingTestMethod));
                        testResults.remove(matchingTestMethod);
                        testResults.get(testMethod).add(testMethod.status);
                    } else {
                        testResults.get(matchingTestMethod).add(testMethod.status);
                    }
                });
                successCount.set(0);
            } else {
                testResults.forEach((key, value) -> value.add(TestNGResults.TestResult.PASS));
                successCount.incrementAndGet();
            }
        });
        return testResults;
    }

    private boolean isRealFailure(Map.Entry<TestNGResults.TestMethod, List<TestNGResults.TestResult>> entry) {
        if (entry.getValue().get(0) == TestNGResults.TestResult.PASS) {
            return false;
        }
        return entry.getValue().stream().filter(value -> value == TestNGResults.TestResult.FAIL).count() > 1;
    }

    private String createJobFragment(int jobIndex, JobDetails fullDetails, List<TestNGResults.TestMethod> failingMethods) {
        String jobFragment = new ClasspathResource("/testFailuresTemplate/jobFailures.html", this.getClass()).getText();
        String rowFragment = new ClasspathResource("/testFailuresTemplate/testFailureRows.html", this.getClass()).getText();

        String filledInJobFragment = jobFragment.replace("#url", fullDetails.url);
        filledInJobFragment = filledInJobFragment.replace("#jobName", fullDetails.name);
        filledInJobFragment = filledInJobFragment.replace("#failingTestCount", String.valueOf(failingMethods.size()));
        filledInJobFragment = filledInJobFragment.replace("#itemId", "item-" + jobIndex);

        StringBuilder rowBuilder = new StringBuilder("");
        int index = 0;
        for (TestNGResults.TestMethod method : failingMethods) {
            String filledInRow = rowFragment.replace("#testName", method.fullTestName());
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
