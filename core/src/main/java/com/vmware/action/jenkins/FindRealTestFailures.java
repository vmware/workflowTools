package com.vmware.action.jenkins;

import java.io.File;
import java.text.SimpleDateFormat;
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
import static com.vmware.util.StringUtils.pluralizeDescription;
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

        log.info("Matched {} views to view name {}", matchingViews.size(), jenkinsConfig.jenkinsView);
        log.info("View names: {}", matchingViews.stream().map(view -> view.name).collect(Collectors.joining(", ")));

        generationDate = new SimpleDateFormat("EEE MMM dd hh:mm:ss aa zzz yyyy").format(new Date());

        File destinationFile = new File(fileSystemConfig.destinationFile);
        validateThatDestinationFileIsDirectoryIfNeeded(matchingViews, destinationFile);

        log.info("Checking last {} builds for tests that are failing in the latest build and have failed in previous builds as well",
                jenkinsConfig.maxJenkinsBuildsToCheck);

        createDbUtilsIfNeeded();
        matchingViews.forEach(view -> saveResultsPageForView(view, destinationFile.isDirectory()));
        purgeVanillaTestResults();
        long elapsedTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);

        if (destinationFile.isDirectory()) {
            String viewListing = createViewListingHtml(matchingViews, elapsedTime);
            fileSystemConfig.destinationFile = fileSystemConfig.destinationFile + File.separator + "index.html";
            log.info("Saving view listing html file to {}", fileSystemConfig.destinationFile);
            IOUtils.write(new File(fileSystemConfig.destinationFile), viewListing);
        }

        log.info("Took {} seconds", elapsedTime);
    }

    private void validateThatDestinationFileIsDirectoryIfNeeded(List<HomePage.View> matchingViews, File destinationFile) {
        if (matchingViews.size() <= 1) {
            return;
        }

        if (!destinationFile.exists()) {
            failIfTrue(!destinationFile.mkdir(), "Failed to create directory " + destinationFile.getAbsolutePath());
        } else {
            failIfTrue(destinationFile.isFile(),
                    destinationFile.getAbsolutePath() + " is a file. Destination file needs to specify a directory as multiple views matched");
        }
    }

    private void createDbUtilsIfNeeded() {
        if (fileSystemConfig.databaseConfigured()) {
            log.debug("Using database driver {} and class {}", fileSystemConfig.databaseDriverFile, fileSystemConfig.databaseDriverClass);
            log.info("Using database {} to store test results",fileSystemConfig.databaseUrl);
            dbUtils = new DbUtils(new File(fileSystemConfig.databaseDriverFile), fileSystemConfig.databaseDriverClass,
                    fileSystemConfig.databaseUrl, fileSystemConfig.dbConnectionProperties());
        }
    }

    private String createViewListingHtml(List<HomePage.View> matchingViews, long elapsedTime) {
        Comparator<HomePage.View> viewComparator = Comparator.comparing(HomePage.View::htmlFileName);
        String viewListingHtml = matchingViews.stream().sorted(viewComparator)
                .map(HomePage.View::listHtmlFragment).collect(Collectors.joining("\n"));
        String viewListingPage = new ClasspathResource("/testFailuresTemplate/viewListing.html", this.getClass()).getText();
        viewListingPage = viewListingPage.replace("#viewPattern", jenkinsConfig.jenkinsView);
        viewListingPage = viewListingPage.replace("#footer", "Generated at " + generationDate + " in " + elapsedTime + " seconds");
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

        final AtomicInteger counter = new AtomicInteger();
        String jobsResultsHtml = failingTestMethods.keySet().stream().sorted(comparing(jobDetails -> jobDetails.name)).map(job -> {
            List<TestResult> failingTests = failingTestMethods.get(job);
            return createJobFragment(counter.getAndIncrement(), job, failingTests);
        }).collect(Collectors.joining("\n"));

        String resultsPage = createTestResultsHtmlPage(view, includeViewsLink, failingTestMethods, jobsResultsHtml);

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

    private String createTestResultsHtmlPage(HomePage.View view, boolean includeViewsLink, Map<Job, List<TestResult>> failingTestMethods, String jobsTestResultsHtml) {
        String resultsPage;
        String footer = "Generated at " + generationDate;
        if (includeViewsLink) {
            footer = "<p/><a href=\"index.html\">Back</a><br/>" + footer;
        }
        if (!failingTestMethods.isEmpty()) {
            String failuresPage = new ClasspathResource("/testFailuresTemplate/testFailuresWebPage.html", this.getClass()).getText();
            resultsPage = failuresPage.replace("#viewName", view.viewNameWithFailureCount());
            resultsPage = resultsPage.replace("#body", jobsTestResultsHtml);
            resultsPage = resultsPage.replace("#footer", footer);
            log.trace("Test Failures for view {}:\n{}", view.name, resultsPage);
        } else {
            String allPassedPage = new ClasspathResource("/testFailuresTemplate/noTestFailures.html", this.getClass()).getText();
            resultsPage = allPassedPage.replace("#viewName", view.name);
            resultsPage = resultsPage.replace("#footer", footer);
        }
        return resultsPage;
    }

    private Map<Job, List<TestResult>> findAllRealFailingTests(JobView jobView) {
        jobView.setDbUtils(dbUtils);
        jobView.populateFromDb();

        Map<Job, List<TestResult>> allFailingTests = new HashMap<>();

        List<Job> usableJobs = jobView.usableJobs(jenkinsConfig.maxJenkinsBuildsToCheck);

        if (usableJobs.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Job> failedJobs = new ArrayList<>();

        usableJobs.stream().parallel().forEach(job -> {
            try {
                job.loadTestResultsFromDb();
                fetchLatestTestResults(job, jobView.lastFetchAmount);
            } catch (Exception e) {
                log.error("Failed to get full job details for {}\n{}", job.name, StringUtils.exceptionAsString(e));
                failedJobs.add(job);
            }
        });

        jobView.lastFetchAmount = jenkinsConfig.maxJenkinsBuildsToCheck;
        jobView.updateInDb();

        log.info("");

        usableJobs.stream().filter(job -> !failedJobs.contains(job)).forEach(job -> {
            job.saveFetchedBuildsInfo();
            boolean passResultsAdded = addPassResultsIfNeeded(job);
            boolean presumedPassResultsAdded = job.addTestResultsToMasterList();
            job.saveTestResultsToDb(passResultsAdded || presumedPassResultsAdded);
            job.removeOldBuilds(jenkinsConfig.maxJenkinsBuildsToCheck);

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

    private boolean addPassResultsIfNeeded(Job job) {
        boolean passResultsAdded = false;
        if (dbUtils != null && job.hasSavedBuilds() && job.lastBuildWasSuccessful() && !job.hasSavedBuild(job.lastStableBuild.buildNumber)) {
            JobBuild stableBuild = jenkinsExecutor.execute(jenkins -> jenkins.getJobBuildDetails(job.name, job.lastStableBuild.buildNumber));
            dbUtils.insert(stableBuild);
            passResultsAdded = job.addPassResultsForSavedTestResults(stableBuild);
        }
        return passResultsAdded;
    }

    private void fetchLatestTestResults(Job job, int lastFetchAmount) {
        job.fetchedResults = Collections.emptyList();
        if (job.lastBuildWasSuccessful()) {
            log.info("No need to fetch latest results for {} as last build {} was successful", job.name, job.lastStableBuild.buildNumber);
        }
        int latestUsableBuildNumber = job.latestUsableBuildNumber();
        if (lastFetchAmount >= jenkinsConfig.maxJenkinsBuildsToCheck && job.hasSavedBuild(latestUsableBuildNumber)) {
            log.info("Saved builds for {} already include latest build {}", job.name, latestUsableBuildNumber);
            return;
        }

        List<JobBuild> builds = Arrays.asList(job.builds);

        final int numberOfBuildsToCheck = builds.size() > jenkinsConfig.maxJenkinsBuildsToCheck ? jenkinsConfig.maxJenkinsBuildsToCheck : builds.size();
        final int lastBuildNumberToCheck = builds.get(numberOfBuildsToCheck - 1).buildNumber;

        List<JobBuild> usableBuilds = builds.stream()
                .filter(build -> build.buildNumber >= lastBuildNumberToCheck && !job.hasSavedBuild(build.buildNumber))
                .collect(toList());
        job.usefulBuilds = usableBuilds.stream().parallel()
                .map(build -> jenkinsExecutor.execute(j -> j.getJobBuildDetails(build)))
                .peek(build -> build.setCommitIdForBuild(jenkinsConfig.commitIdInDescriptionPattern))
                .collect(toList());
        job.usefulBuilds.removeIf(build -> build.status != SUCCESS && build.status != UNSTABLE);

        List<Supplier<TestResults>> usableResultSuppliers = job.usefulBuilds.stream()
                .map(build -> (Supplier<TestResults>) () -> jenkinsExecutor.execute(j -> j.getJobBuildTestResults(build)))
                .collect(toList());

        if (job.usefulBuilds.isEmpty()) {
            log.info("Builds for {} already saved, checked for last {} of {} usable builds. Last build number checked was {}", job.name,
                    numberOfBuildsToCheck, builds.size(), lastBuildNumberToCheck);
        } else {
            String buildsToFetch = job.usefulBuilds.stream().map(JobBuild::buildNumber).collect(Collectors.joining(","));
            log.info("Fetching {} {} for {}. Checking last {} of {} builds. Last build number checked was {}",
                    pluralizeDescription(usableBuilds.size(), "build"), buildsToFetch, job.name, numberOfBuildsToCheck,
                    builds.size(), lastBuildNumberToCheck);
            job.fetchedResults = usableResultSuppliers.stream().parallel().map(Supplier::get).filter(Objects::nonNull).collect(toList());
        }
    }

    private String createJobFragment(int jobIndex, Job fullDetails, List<TestResult> failingMethods) {
        String jobFragment = new ClasspathResource("/testFailuresTemplate/jobFailures.html", this.getClass()).getText();
        String rowFragment = new ClasspathResource("/testFailuresTemplate/testFailureRows.html", this.getClass()).getText();

        String filledInJobFragment = jobFragment.replace("#url", fullDetails.url);
        filledInJobFragment = filledInJobFragment.replace("#runningBuildLink", fullDetails.runningBuildLink());
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

    private void purgeVanillaTestResults() {
        if (dbUtils == null) {
            return;
        }
        String tableName = StringUtils.convertToDbName(TestResult.class.getSimpleName());
        int purgedRecords = dbUtils.delete("DELETE FROM " + tableName + " WHERE STATUS = '" + TestResult.TestStatus.PASS.name()
                + "' and coalesce(failed_builds, '') = '' and coalesce(skipped_builds, '') = ''");
        if (purgedRecords > 0) {
            log.info("Purged {} test results from database that have no failure information", purgedRecords);
        }
    }

}
