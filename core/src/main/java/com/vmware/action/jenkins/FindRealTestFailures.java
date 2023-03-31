package com.vmware.action.jenkins;

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
import com.vmware.util.CollectionUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.StopwatchUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.collection.BlockingExecutorService;
import com.vmware.util.db.DbUtils;
import com.vmware.util.logging.Padder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vmware.BuildStatus.FAILURE;
import static com.vmware.BuildStatus.SUCCESS;
import static com.vmware.BuildStatus.UNSTABLE;
import static com.vmware.util.StringUtils.pluralize;
import static com.vmware.util.StringUtils.pluralizeDescription;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

@ActionDescription("Finds real test failures for a Jenkins View. Real failures are tests that are continuously failing as opposed to one offs.")
public class FindRealTestFailures extends BaseAction {

    protected static final String LINK_IN_NEW_TAB = "target=\"_blank\" rel=\"noopener noreferrer\"";

    private BlockingExecutorService<Jenkins> jenkinsExecutor;
    private String generationDate;
    private DbUtils dbUtils;

    public FindRealTestFailures(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (!jenkinsConfig.createTestFailuresDatabase) {
            Stream.of("jenkinsView", "destinationFile").forEach(this::failIfUnset);
        }
    }

    @Override
    public void process() {
        if (jenkinsConfig.createTestFailuresDatabase) {
            createDatabase();
            return;
        }
        generationDate = new SimpleDateFormat("EEE MMM dd hh:mm:ss aa zzz yyyy").format(new Date());
        log.info("Checking for failing tests matching view pattern {} on {}", jenkinsConfig.jenkinsView, generationDate);
        File destinationFile = new File(fileSystemConfig.destinationFile);
        createDbUtilsIfNeeded();
        HomePage homePage;
        if (jenkinsConfig.regenerateHtml) {
            log.info("Regenerating failing tests html pages just from database test results");
            homePage = new HomePage();
            homePage.setDbUtils(dbUtils);
            homePage.populateFromDatabase(jenkinsConfig.jenkinsView);
        } else {
            this.jenkinsExecutor = new BlockingExecutorService<>(6, () -> {
                LoggerFactory.getLogger(FindRealTestFailures.class).debug("Creating new jenkins service");
                return serviceLocator.newJenkins();
            });
            homePage = serviceLocator.getJenkins().getHomePage();
        }
        List<HomePage.View> matchingViews = Arrays.stream(homePage.views).filter(view -> view.matches(jenkinsConfig.jenkinsView)).collect(toList());

        StopwatchUtils.Stopwatch stopwatch = StopwatchUtils.start();
        if (matchingViews.isEmpty()) {
            exitDueToFailureCheck("No views found for name " + jenkinsConfig.jenkinsView);
        }

        log.info("Matched {} views to view name {}", matchingViews.size(), jenkinsConfig.jenkinsView);
        log.info("View names: {}", matchingViews.stream().map(view -> view.name).collect(Collectors.joining(", ")));

        validateThatDestinationFileIsDirectoryIfNeeded(matchingViews, destinationFile);

        if (this.jenkinsExecutor != null) {
            log.info("Checking last {} builds for tests that are failing in the latest build and have failed in previous builds as well",
                    jenkinsConfig.maxJenkinsBuildsToCheck);
            if (jenkinsConfig.forceRefetch) {
                log.info("forceRefetch is set to true so all builds will be refetched");
            }
            if (StringUtils.isNotBlank(jenkinsConfig.jobWithArtifact)) {
                log.info("Only using jobs matching pattern {}", jenkinsConfig.jobWithArtifact);
            }

            matchingViews.forEach(view -> saveResultsPageForView(view, destinationFile.isDirectory()));
        } else {
            matchingViews.forEach(view -> createJobResultsHtmlPages(view, destinationFile.isDirectory(), homePage.jobTestMap(view.name)));
        }
        long elapsedTime = stopwatch.elapsedTime(TimeUnit.SECONDS);

        if (destinationFile.isDirectory()) {
            String viewListing = createViewListingHtml(matchingViews, elapsedTime);
            fileSystemConfig.destinationFile = fileSystemConfig.destinationFile + File.separator + "index.html";
            log.info("Saving view listing html file to {}", fileSystemConfig.destinationFile);
            IOUtils.write(new File(fileSystemConfig.destinationFile), viewListing);
        }

        if (dbUtils != null) {
            dbUtils.closeConnection();
        }

        if (elapsedTime > 0) {
            log.info("Took {} seconds", elapsedTime);
        }
    }

    private void createDatabase() {
        createDbUtilsIfNeeded();
        if (dbUtils == null) {
            exitDueToFailureCheck("Database connection not configured");
        }
        log.info("Executing database creation script");
        dbUtils.executeSqlScript(new ClasspathResource("/testFailuresTemplate/databaseDdl.sql", this.getClass()).getText());
        dbUtils.closeConnection();
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
        if (!fileSystemConfig.databaseConfigured()) {
            return;
        }
        Stream.of("databaseDriverFile", "databaseDriverClass").forEach(this::failIfUnset);
        log.debug("Using database driver {} and class {}", fileSystemConfig.databaseDriverFile, fileSystemConfig.databaseDriverClass);
        log.info("Using database {} for test results", fileSystemConfig.databaseUrl);
        dbUtils = new DbUtils(new File(fileSystemConfig.databaseDriverFile), fileSystemConfig.databaseDriverClass,
                fileSystemConfig.databaseUrl, fileSystemConfig.dbConnectionProperties());
        dbUtils.createConnection();
    }

    private String createViewListingHtml(List<HomePage.View> matchingViews, long elapsedTime) {
        Comparator<HomePage.View> viewComparator = Comparator.comparing(HomePage.View::htmlFileName);
        String viewListingHtml = matchingViews.stream().sorted(viewComparator)
                .map(HomePage.View::listHtmlFragment).collect(Collectors.joining("\n"));
        long totalFailingTests = matchingViews.stream().mapToLong(view -> view.failingTestsCount).sum();
        String viewListingPage = new ClasspathResource("/testFailuresTemplate/viewListing.html", this.getClass()).getText();
        viewListingPage = viewListingPage.replace("#failingTestCount", String.format("%,d", totalFailingTests));
        viewListingPage = viewListingPage.replace("#viewPattern", jenkinsConfig.jenkinsView);

        String footer = "";
        if (StringUtils.isNotBlank(jenkinsConfig.testMethodNameSearchUrl)) {
            footer += "<p/><a href=\"" + jenkinsConfig.testMethodNameSearchUrl + "\" " + LINK_IN_NEW_TAB + ">Search test methods</a><br/>";
        }
        footer += String.format("Generated at %s in %s seconds", generationDate, elapsedTime);
        if (dbUtils != null) {
            footer += String.format(" (%,d saved test results)", dbUtils.queryUnique(Integer.class, "SELECT COUNT(*) FROM TEST_RESULT"));
        }

        viewListingPage = viewListingPage.replace("#footer", footer);
        return viewListingPage.replace("#body", viewListingHtml);
    }

    private void saveResultsPageForView(HomePage.View view, boolean includeViewsLink) {
        Padder viewPadder = new Padder(view.name);
        viewPadder.infoTitle();

        Jenkins jenkins = serviceLocator.getJenkins();
        JobView jobView = jenkins.getFullViewDetails(view.url);
        log.info("Checking {} jobs for test failures", jobView.jobs.length);

        try {
            addFailingTestForView(jobView);
        } catch (Exception e) {
            log.error("Failed to create page for view {}\n{}", view.name, StringUtils.exceptionAsString(e));
            view.failingTestsGenerationException = e;
            viewPadder.infoTitle();
            return;
        }

        view.failingTestsCount = jobView.failingTestCount();
        log.info("{} failing tests found for view {}", view.failingTestsCount, view.name);

        createJobResultsHtmlPages(view, includeViewsLink, jobView.getJobTestMap());
        viewPadder.infoTitle();
    }

    private void createJobResultsHtmlPages(HomePage.View view, boolean includeViewsLink, Map<Job, List<TestResult>> jobTestResults) {
        final AtomicInteger counter = new AtomicInteger();

        Comparator<Map.Entry<Job, List<TestResult>>> groupComparator = (first, second) -> {
            if (jenkinsConfig.groupByNamePatterns == null || jenkinsConfig.groupByNamePatterns.length == 0) {
                return 0;
            }

            int noMatchIndex = jenkinsConfig.groupByNamePatterns.length + 1;
            Integer firstMatchIndex = IntStream.range(0, jenkinsConfig.groupByNamePatterns.length)
                    .filter(index  -> first.getKey().name.matches(jenkinsConfig.groupByNamePatterns[index])).findFirst().orElse(noMatchIndex);
            Integer secondMatchIndex = IntStream.range(0, jenkinsConfig.groupByNamePatterns.length)
                    .filter(index  -> second.getKey().name.matches(jenkinsConfig.groupByNamePatterns[index])).findFirst().orElse(noMatchIndex);

            return firstMatchIndex.compareTo(secondMatchIndex);

        };
        List<String> jobsResultsHtml = jobTestResults.entrySet().stream()
                .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
                .sorted(groupComparator.thenComparing(entry -> entry.getKey().name))
                .map(entry -> createJobFragment(counter.getAndIncrement(), view.url, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        List<String> jobsPassingHtml = jobTestResults.entrySet().stream()
                .filter(entry -> CollectionUtils.isEmpty(entry.getValue()) && entry.getKey().lastBuildWasSuccessful())
                .sorted(groupComparator.thenComparing(entry -> entry.getKey().name))
                .map(entry -> createJobFragment(counter.getAndIncrement(), view.url, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        Map<Job, List<TestResult>> inconsistentFailingTests = jobTestResults.entrySet().stream()
                .filter(entry -> CollectionUtils.isEmpty(entry.getValue()) && !entry.getKey().lastBuildWasSuccessful())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getKey().latestFailingTests(jenkinsConfig.maxJenkinsBuildsToCheck)));


        List<String> inconsistentJobsHtml = inconsistentFailingTests.entrySet().stream()
                .map(entry -> createJobFragment(counter.getAndIncrement(), view.url, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        view.inconsistentFailingTestCount = inconsistentFailingTests.values().stream().mapToLong(List::size).sum();
        view.inconsistentFailingJobsCount = inconsistentFailingTests.size();

        String resultsPage = createTestResultsHtmlPage(view, includeViewsLink, jobsResultsHtml, inconsistentJobsHtml, jobsPassingHtml);

        File destinationFile = new File(fileSystemConfig.destinationFile);
        if (destinationFile.exists() && destinationFile.isDirectory()) {
            File failurePageFile = new File(fileSystemConfig.destinationFile + File.separator + view.htmlFileName());
            log.info("Saving test results to {}", failurePageFile);
            IOUtils.write(failurePageFile, resultsPage);
        } else {
            log.info("Saving test results to {}", destinationFile);
            IOUtils.write(destinationFile, resultsPage);
        }
    }

    private String createTestResultsHtmlPage(HomePage.View view, boolean includeViewsLink, List<String> consistentFailuresJobsHtml,
                                             List<String> inconsistentFailuresJobsHtml, List<String> jobsPassingHtml) {
        String resultsPage;
        String footer = "";
        if (StringUtils.isNotBlank(jenkinsConfig.testMethodNameSearchUrl)) {
            String queryCharacter = jenkinsConfig.testMethodNameSearchUrl.contains("?") ? "&" : "?";
            String searchUrl = jenkinsConfig.testMethodNameSearchUrl + queryCharacter + "viewName=" + view.name;
            footer += "<p/><a href=\"" + searchUrl + "\" " + LINK_IN_NEW_TAB + ">Search test methods</a><br/>";
        }
        if (includeViewsLink) {
            footer += "<p/><a href=\"index.html\">Back</a><br/>";
        }
        footer += "Generated at " + generationDate;
        if (consistentFailuresJobsHtml.isEmpty() && inconsistentFailuresJobsHtml.isEmpty()) {
            String allPassedPage = new ClasspathResource("/testFailuresTemplate/noTestFailures.html", this.getClass()).getText();
            resultsPage = allPassedPage.replace("#viewName", view.name);
            resultsPage = resultsPage.replace("#viewUrl", view.url);
            resultsPage = resultsPage.replace("#footer", footer);
        } else {
            String failuresPage = new ClasspathResource("/testFailuresTemplate/testFailuresWebPage.html", this.getClass()).getText();
            resultsPage = failuresPage.replace("#viewName", view.name);
            resultsPage = resultsPage.replace("#viewUrl", view.url);
            resultsPage = resultsPage.replace("#viewTotalFailures", String.valueOf(view.failingTestsCount));
            resultsPage = resultsPage.replace("#consistentFailuresJobsCount", String.valueOf(consistentFailuresJobsHtml.size()));

            resultsPage = resultsPage.replace("#body", String.join("\n", consistentFailuresJobsHtml));

            resultsPage = resultsPage.replace("#inconsistentFailuresJobsCount", String.valueOf(inconsistentFailuresJobsHtml.size()));
            resultsPage = resultsPage.replace("#inconsistentFailuresJobs", String.join("\n", inconsistentFailuresJobsHtml));
            resultsPage = resultsPage.replace("#passingJobsCount", String.valueOf(jobsPassingHtml.size()));
            resultsPage = resultsPage.replace("#passingJobs", String.join("\n", jobsPassingHtml));

            resultsPage = resultsPage.replace("#footer", footer);
            log.trace("Test Failures for view {}:\n{}", view.name, resultsPage);
        }

        return resultsPage;
    }

    private void addFailingTestForView(JobView jobView) {
        jobView.setDbUtils(dbUtils);
        jobView.populateFromDb();

        List<Job> usableJobs = jobView.usableJobs(jenkinsConfig.jobWithArtifact);

        if (usableJobs.isEmpty()) {
            return;
        }

        List<Job> failedJobs = new ArrayList<>();

        usableJobs.stream().parallel().forEach(job -> {
            try {
                job.loadTestResultsFromDb();
                fetchLatestTestResults(job);
            } catch (Exception e) {
                log.error("Failed to get full job details for {}\n{}", job.name, StringUtils.exceptionAsString(e));
                failedJobs.add(job);
            }
        });

        jobView.lastFetchAmount = jenkinsConfig.maxJenkinsBuildsToCheck;

        log.info("");

        jobView.updateInDb();
        List<Job> jobsToCheckForFailingTests = usableJobs.stream().filter(job -> !failedJobs.contains(job)).collect(toList());

        StopwatchUtils.Stopwatch stopwatch = StopwatchUtils.start();

        jobsToCheckForFailingTests.forEach(job -> {
            job.saveFetchedBuildsInfo();
            job.addTestResultsToMasterList();
            job.saveTestResultsToDb();
            job.removeOldBuilds(jenkinsConfig.maxJenkinsBuildsToCheck);
        });

        jobsToCheckForFailingTests.forEach(job -> {
            List<TestResult> failingTests = job.createFailingTestsList(jenkinsConfig.maxJenkinsBuildsToCheck);
            if (failingTests.isEmpty()) {
                log.info("No consistently failing tests found for {}", job.name);
                jobView.addFailingTests(job, failingTests);
            } else {
                log.info("Found {} for {}", pluralize(failingTests.size(), "consistently failing test"), job.name);
                jobView.addFailingTests(job, failingTests);
            }
        });

        log.info("Created failing test lists for {} jobs in {} milliseconds", jobsToCheckForFailingTests.size(), stopwatch.elapsedTime());
    }

    private void fetchLatestTestResults(Job job) {
        job.fetchedResults = Collections.emptyList();
        List<JobBuild> builds = Arrays.asList(job.builds);

        final int numberOfBuildsToCheck = builds.size() > jenkinsConfig.maxJenkinsBuildsToCheck ? jenkinsConfig.maxJenkinsBuildsToCheck : builds.size();
        final int lastBuildNumberToCheck = builds.get(numberOfBuildsToCheck - 1).buildNumber;

        List<JobBuild> usableBuilds = builds.stream()
                .filter(build -> build.buildNumber >= lastBuildNumberToCheck
                        && (jenkinsConfig.forceRefetch || !job.buildIsTooOld(build.buildNumber, jenkinsConfig.maxJenkinsBuildsToCheck)))
                .collect(toList());

        job.usefulBuilds = usableBuilds.stream().parallel()
                .map(build -> jenkinsExecutor.execute(j -> j.getJobBuildDetails(build)))
                .peek(build -> build.setCommitIdForBuild(jenkinsConfig.commitIdInDescriptionPattern))
                .collect(toList());

        job.usefulBuilds.removeIf(build -> build.status == null);

        List<Supplier<TestResults>> usableResultSuppliers = job.usefulBuilds.stream()
                .filter(build -> build.status == SUCCESS || build.status == UNSTABLE || build.status == FAILURE)
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

    private String createJobFragment(int jobIndex, String viewUrl, Job fullDetails, List<TestResult> failingMethods) {
        String jobFragmentFile = failingMethods.isEmpty() ? "/testFailuresTemplate/jobNoFailures.html" : "/testFailuresTemplate/jobFailures.html";
        String jobFragment = new ClasspathResource(jobFragmentFile, this.getClass()).getText();
        String rowFragment = new ClasspathResource("/testFailuresTemplate/testFailureRows.html", this.getClass()).getText();

        String jobPath = StringUtils.substringAfterLast(fullDetails.url, "/job/");
        String jobUrlWithViewName = UrlUtils.addRelativePaths(viewUrl, "job", jobPath);

        String filledInJobFragment = jobFragment.replace("#url", jobUrlWithViewName);
        filledInJobFragment = filledInJobFragment.replace("#runningBuildLink", fullDetails.runningBuildLink(viewUrl));
        filledInJobFragment = filledInJobFragment.replace("#latestBuildLink", fullDetails.latestBuildLink(viewUrl));
        filledInJobFragment = filledInJobFragment.replace("#jobName", fullDetails.name);
        filledInJobFragment = filledInJobFragment.replace("#failingTestCount", pluralize(failingMethods.size(), "failing test"));
        filledInJobFragment = filledInJobFragment.replace("#itemId", "item-" + jobIndex);

        StringBuilder rowBuilder = new StringBuilder();
        int index = 0;
        for (TestResult method : failingMethods.stream().sorted(comparingLong(TestResult::getStartedAt)).collect(toList())) {
            String filledInRow = rowFragment.replace("#testName", method.fullTestNameForDisplay());
            filledInRow = filledInRow.replace("#testResultLinks", method.testLinks(viewUrl, jenkinsConfig.commitComparisonUrl));
            filledInRow = filledInRow.replace("#itemId", "item-" + jobIndex + "-" + index++);
            filledInRow = filledInRow.replace("#exception", method.exception != null ? method.exception.replace("\n", "<br/>") : "");
            rowBuilder.append(filledInRow).append("\n");
        }
        filledInJobFragment = filledInJobFragment.replace("#body", rowBuilder.toString());
        return filledInJobFragment;
    }

}
