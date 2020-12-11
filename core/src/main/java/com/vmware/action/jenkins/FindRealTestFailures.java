package com.vmware.action.jenkins;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.HomePage;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobView;
import com.vmware.jenkins.domain.TestResult;
import com.vmware.jenkins.domain.TestResults;
import com.vmware.util.ClasspathResource;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.collection.BlockingExecutorService;
import com.vmware.util.db.DbUtils;
import com.vmware.util.logging.Padder;

import org.slf4j.LoggerFactory;

import static com.vmware.BuildStatus.SUCCESS;
import static com.vmware.BuildStatus.UNSTABLE;
import static com.vmware.util.StringUtils.pluralize;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

@ActionDescription("Finds real test failures for a Jenkins View. Real failures are tests that are continuously failing as opposed to one offs.")
public class FindRealTestFailures extends BaseAction {

    private BlockingExecutorService<Jenkins> jenkinsExecutor;
    private String generationDate;
    private DbUtils dbUtils;

    public FindRealTestFailures(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("jenkinsView", "destinationFile");
    }

    @Override
    public void process() {
        log.info("Checking for failing tests matching view pattern {} on {}", jenkinsConfig.jenkinsView, new Date().toString());
        this.jenkinsExecutor = new BlockingExecutorService<>(6, () -> {
            LoggerFactory.getLogger(FindRealTestFailures.class).debug("Creating new service");
            return serviceLocator.newJenkins();
        });
        long startTime = System.currentTimeMillis();

        HomePage homePage = serviceLocator.getJenkins().getHomePage();

        List<HomePage.View> matchingViews = Arrays.stream(homePage.views).filter(view -> view.matches(jenkinsConfig.jenkinsView)).collect(toList());

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
                    destinationFile.getAbsolutePath() + " is a file. Destination file needs to specify a directory as multiple views matched");
        }

        if (fileSystemConfig.databaseConfigured()) {
            log.debug("Using database driver {} and class {}", fileSystemConfig.databaseDriverFile, fileSystemConfig.databaseDriverClass);
            log.info("Using database {} to store test results",fileSystemConfig.databaseUrl);
            dbUtils = new DbUtils(new File(fileSystemConfig.databaseDriverFile), fileSystemConfig.databaseDriverClass,
                    fileSystemConfig.databaseUrl, fileSystemConfig.dbConnectionProperties());
        }

        matchingViews.forEach(view -> saveResultsPageForView(view, destinationFile.isDirectory()));

        if (destinationFile.isDirectory()) {
            String viewListing = createViewListingHtml(matchingViews);
            fileSystemConfig.destinationFile = fileSystemConfig.destinationFile + File.separator + "index.html";
            log.info("Saving view listing html file to {}", fileSystemConfig.destinationFile);
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

    private void saveResultsPageForView(HomePage.View view, boolean includeViewsLink) {
        Padder viewPadder = new Padder(view.name);
        viewPadder.infoTitle();

        Jenkins jenkins = serviceLocator.getJenkins();
        JobView jobView = jenkins.getFullViewDetails(view.url);
        log.info("Checking {} jobs for test failures", jobView.jobs.length);

        final Map<Job, List<TestResult>> failingTestMethods;
        try {
            failingTestMethods = findAllRealFailingTests(jobView);
        } catch (Exception e) {
            log.error("Failed to create page for view {}\n{}", view.name, StringUtils.exceptionAsString(e));
            view.failingTestsGenerationException = e;
            viewPadder.infoTitle();
            return;
        }

        view.failingTestsCount = failingTestMethods.values().stream().mapToInt(List::size).sum();
        log.info("{} failing tests found for view {}", view.failingTestsCount, view.name);

        final StringBuilder jobFragments = new StringBuilder("");
        final AtomicInteger counter = new AtomicInteger();
        failingTestMethods.keySet().stream().sorted(comparing(jobDetails -> jobDetails.name)).forEach(key -> {
            List<TestResult> failingTests = failingTestMethods.get(key);
            String jobFragment = createJobFragment(counter.getAndIncrement(), key, failingTests);
            if (jobFragments.length() > 0) {
                jobFragments.append("\n");
            }
            jobFragments.append(jobFragment);
        });

        String resultsPage;
        String footer = "Generated at " + generationDate;
        if (includeViewsLink) {
            footer = "<p/><a href=\"index.html\">Back</a><br/>" + footer;
        }
        if (!failingTestMethods.isEmpty()) {
            String failuresPage = new ClasspathResource("/testFailuresTemplate/testFailuresWebPage.html", this.getClass()).getText();
            resultsPage = failuresPage.replace("#viewName", view.viewNameWithFailureCount());
            resultsPage = resultsPage.replace("#body", jobFragments.toString());
            resultsPage = resultsPage.replace("#footer", footer);
            log.trace("Test Failures for view {}:\n{}", view.name, resultsPage);
        } else {
            String allPassedPage = new ClasspathResource("/testFailuresTemplate/noTestFailures.html", this.getClass()).getText();
            resultsPage = allPassedPage.replace("#viewName", view.name);
            resultsPage = resultsPage.replace("#footer", footer);
        }

        File destinationFile = new File(fileSystemConfig.destinationFile);
        if (destinationFile.exists() && destinationFile.isDirectory()) {
            File failurePageFile = new File(fileSystemConfig.destinationFile + File.separator + view.htmlFileName());
            log.info("Saving test results to {}", failurePageFile);
            IOUtils.write(failurePageFile, resultsPage);
        } else {
            log.info("Saving test results to {}", destinationFile);
            IOUtils.write(destinationFile, resultsPage);
        }
        viewPadder.infoTitle();
    }

    private Map<Job, List<TestResult>> findAllRealFailingTests(JobView jobView) {
        jobView.populateFromDb(dbUtils);

        Map<Job, List<TestResult>> allFailingTests = new HashMap<>();

        List<Job> usableJobs = jobView.usableJobs(dbUtils, jenkinsConfig.maxJenkinsBuildsToCheck);

        if (usableJobs.isEmpty()) {
            return Collections.emptyMap();
        }

        usableJobs.stream().parallel().forEach(job -> {
            try {
                addTestResultsToJob(job, jobView.lastFetchAmount);
            } catch (Exception e) {
                log.error("Failed to get full job details for {}\n{}", job.name, StringUtils.exceptionAsString(e));
                throw e;
            }
        });

        jobView.lastFetchAmount = jenkinsConfig.maxJenkinsBuildsToCheck;
        if (dbUtils != null) {
            dbUtils.update(jobView);
        }

        log.info("");

        usableJobs.forEach(job -> {
            job.addTestResultsToMasterList();
            job.saveTestResultsToDb(dbUtils);
            job.removeOldBuilds(dbUtils, jenkinsConfig.maxJenkinsBuildsToCheck);
            List<TestResult> failingTests = job.createFailingTestsList(jenkinsConfig.maxJenkinsBuildsToCheck);
            if (failingTests.isEmpty()) {
                log.info("No consistently failing tests found for {}", job.name);
            } else {
                log.info("Found {} for {}", pluralize(failingTests.size(), "consistently failing test"), job.name);
                allFailingTests.put(job, failingTests);
            }
        });

        return allFailingTests;
    }

    private void addTestResultsToJob(Job job, int lastFetchAmount) {
        List<JobBuild> savedBuilds = dbUtils.query(JobBuild.class, "SELECT * from JOB_BUILD WHERE JOB_ID = ?", job.id);
        job.testResults = new ArrayList<>();
        job.testResults.addAll(loadTestResultsFromDb(job, savedBuilds));

        int latestUsableBuildNumber = job.latestUsableBuildNumber();
        if (lastFetchAmount >= jenkinsConfig.maxJenkinsBuildsToCheck && savedBuilds.stream().anyMatch(build -> build.buildNumber == latestUsableBuildNumber)) {
            log.info("Saved builds for {} already include latest build {}", job.name, latestUsableBuildNumber);
            return;
        }

        Job fullDetails = jenkinsExecutor.execute(j -> j.getJobDetails(job.getFullInfoUrl()));
        job.builds = fullDetails.builds; // full details includes build statuses

        List<JobBuild> usefulBuilds = Arrays.stream(job.builds).filter(build -> build.status == SUCCESS || build.status == UNSTABLE)
                .sorted((first, second) -> Integer.compare(second.buildNumber, first.buildNumber))
                .peek(build -> build.setCommitIdForBuild(jenkinsConfig.commitIdInDescriptionPattern)).collect(toList());
        job.usefulBuilds = usefulBuilds;

        if (usefulBuilds.isEmpty()) {
            log.info("No usable builds found for {}", job.name);
            return;
        }

        if (usefulBuilds.get(0).status == SUCCESS) {
            log.info("Most recent build for {} passed", job.name);
            return;
        }

        final int numberOfBuildsToCheck = usefulBuilds.size() > jenkinsConfig.maxJenkinsBuildsToCheck ? jenkinsConfig.maxJenkinsBuildsToCheck : usefulBuilds.size();
        final int lastBuildNumberToCheck = usefulBuilds.get(numberOfBuildsToCheck - 1).buildNumber;

        List<Supplier<TestResults>> usableResultSuppliers = usefulBuilds.stream().map(build -> {
            if (build.buildNumber < lastBuildNumberToCheck) {
                return null;
            }
            return fetchBuildIfNeeded(build, savedBuilds);
            }).filter(Objects::nonNull).collect(toList());

        if (usableResultSuppliers.isEmpty()) {
            log.info("Builds for {} already saved, checked for last {} of {} usable builds. Lase build number checked was {}", job.name,
                    numberOfBuildsToCheck, usefulBuilds.size(), lastBuildNumberToCheck);
        } else {
            log.info("Fetching {} for {}. Checked last {} of {} usable builds. Last build number checked was {}",
                    pluralize(usableResultSuppliers.size(), "new build"), job.name, numberOfBuildsToCheck, usefulBuilds.size(), lastBuildNumberToCheck);
            job.fetchedResults = usableResultSuppliers.stream().parallel().map(Supplier::get).collect(toList());
        }


    }


    private List<TestResult> loadTestResultsFromDb(Job job, List<JobBuild> savedBuilds) {
        if (dbUtils == null) {
            return Collections.emptyList();
        }
        try (Connection connection = dbUtils.createConnection()) {
            List<TestResult> testResults = dbUtils.query(connection, TestResult.class, "SELECT tr.* from TEST_RESULT tr"
                    + " JOIN JOB_BUILD jb ON tr.job_build_id = jb.id WHERE jb.JOB_ID = ? ORDER BY ID ASC", job.id);
            Set<String> usedUrls = new HashSet<>();
            savedBuilds.forEach(build -> testResults.stream().filter(result -> result.jobBuildId.equals(build.id)).forEach(result -> {
                result.commitId = build.commitId;
                result.buildNumber = build.buildNumber;
                result.setUrlForTestMethod(build.getTestReportsUIUrl(), usedUrls);
                usedUrls.add(result.url);
            }));
            return testResults;
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private Supplier<TestResults> fetchBuildIfNeeded(JobBuild build, List<JobBuild> savedBuilds) {
        if (savedBuilds.stream().anyMatch(savedBuild -> savedBuild.url.equals(build.url))) {
            return null;
        }
        return () -> jenkinsExecutor.execute(j -> j.getJobBuildTestResults(build));
    }

    private String createJobFragment(int jobIndex, Job fullDetails, List<TestResult> failingMethods) {
        String jobFragment = new ClasspathResource("/testFailuresTemplate/jobFailures.html", this.getClass()).getText();
        String rowFragment = new ClasspathResource("/testFailuresTemplate/testFailureRows.html", this.getClass()).getText();

        String filledInJobFragment = jobFragment.replace("#url", fullDetails.url);
        filledInJobFragment = filledInJobFragment.replace("#jobName", fullDetails.name);
        filledInJobFragment = filledInJobFragment.replace("#failingTestCount", pluralize(failingMethods.size(), "failing test"));
        filledInJobFragment = filledInJobFragment.replace("#itemId", "item-" + jobIndex);

        StringBuilder rowBuilder = new StringBuilder("");
        int index = 0;
        for (TestResult method : failingMethods.stream().sorted(comparingLong(TestResult::getStartedAt)).collect(toList())) {
            String filledInRow = rowFragment.replace("#testName", method.fullTestNameWithSkipInfo());
            filledInRow = filledInRow.replace("#testResultLinks", method.testLinks(jenkinsConfig.commitComparisonUrl));
            filledInRow = filledInRow.replace("#itemId", "item-" + jobIndex + "-" + index++);
            filledInRow = filledInRow.replace("#exception", method.exception != null ? method.exception.replace("\n", "<br/>") : "");
            rowBuilder.append(filledInRow).append("\n");
        }
        filledInJobFragment = filledInJobFragment.replace("#body", rowBuilder.toString());
        return filledInJobFragment;
    }
}
