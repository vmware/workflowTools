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
import com.vmware.util.MatcherUtils;
import com.vmware.util.StopwatchUtils;
import com.vmware.util.StringUtils;
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

@ActionDescription("Finds test and config failures for a Jenkins View.")
public class FindTestFailures extends BaseAction {

    protected static final String LINK_IN_NEW_TAB = "target=\"_blank\" rel=\"noopener noreferrer\"";

    private BlockingExecutorService<Jenkins> jenkinsExecutor;
    private String generationDate;
    private DbUtils dbUtils;

    public FindTestFailures(WorkflowConfig config) {
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
        generationDate = new SimpleDateFormat("EEE MMM dd hh:mm aa zzz").format(new Date());
        log.info("Checking for failing tests matching view pattern {} on {}, tests are considered failing if there are {} or more failures",
                jenkinsConfig.jenkinsView, generationDate, jenkinsConfig.numberOfFailuresNeededToBeConsistentlyFailing);
        File destinationFile = new File(fileSystemConfig.destinationFile);
        createDbUtilsIfNeeded();
        HomePage homePage;
        if (jenkinsConfig.regenerateHtml) {
            log.info("Regenerating failing tests html pages just from database test results");
            homePage = new HomePage();
            homePage.setDbUtils(dbUtils);
            homePage.populateFromDb(jenkinsConfig.jenkinsView, jenkinsConfig.numberOfFailuresNeededToBeConsistentlyFailing);
        } else {
            this.jenkinsExecutor = new BlockingExecutorService<>(6, () -> {
                LoggerFactory.getLogger(FindTestFailures.class).debug("Creating new jenkins service");
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

        if (jenkinsConfig.maxJenkinsBuildsToCheck > jenkinsConfig.maxJenkinsBuildsToKeep) {
            log.info("Setting max builds to keep to {} as that is the amount of builds to check", jenkinsConfig.maxJenkinsBuildsToCheck);
            jenkinsConfig.maxJenkinsBuildsToKeep = jenkinsConfig.maxJenkinsBuildsToCheck;
        }

        if (this.jenkinsExecutor != null) {
            log.info("Checking last {} for tests that are failing in the latest build and have failed in previous builds as well",
                    StringUtils.pluralize(jenkinsConfig.maxJenkinsBuildsToCheck, "build"));
            if (dbUtils != null) {
                log.info("Keeping last {} in test results database", StringUtils.pluralize(jenkinsConfig.maxJenkinsBuildsToKeep, "build"));
            }
            if (jenkinsConfig.refetchCount > 0) {
                log.info("refetchCount is set to {} so {} builds will be refetched", jenkinsConfig.refetchCount, jenkinsConfig.refetchCount);
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
            footer += "<p/>" + constructAnchorUsingNewTab(jenkinsConfig.testMethodNameSearchUrl, "Search saved results") + "<br/>";
        }
        footer += String.format("Generated at %s in %s seconds", generationDate, elapsedTime);
        if (dbUtils != null) {
            String databaseSize = StringUtils.isNotBlank(fileSystemConfig.databaseSizeQuery) ?
                    " " + StringUtils.humanReadableSize(dbUtils.queryUnique(String.class, fileSystemConfig.databaseSizeQuery)) : "";
            footer += String.format(" (%,d saved results%s)",dbUtils.queryUnique(Integer.class,
                    "SELECT COUNT(*) FROM TEST_RESULT"), databaseSize);
        }

        viewListingPage = viewListingPage.replace("#footer", footer);
        return viewListingPage.replace("#body", viewListingHtml);
    }

    private void saveResultsPageForView(HomePage.View view, boolean includeViewsLink) {
        Padder viewPadder = new Padder(view.name);
        viewPadder.infoTitle();

        Jenkins jenkins = serviceLocator.getJenkins();
        JobView jobView = jenkins.getFullViewDetails(view.url);
        log.info("Checking {} jobs for failures", jobView.jobs.length);

        try {
            addFailingTestForView(jobView);
        } catch (Exception e) {
            log.error("Failed to create page for view {}\n{}", view.name, StringUtils.exceptionAsString(e));
            view.failingTestsGenerationException = e;
            viewPadder.infoTitle();
            return;
        }

        view.failingTestsCount = jobView.failingTestCount(jenkinsConfig.numberOfFailuresNeededToBeConsistentlyFailing);
        view.failingTestMethodsCount = jobView.failingTestMethodCount();
        view.totalTestMethodsCount = jobView.totalTestMethodCount();
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

        Map<Job, List<TestResult>> failingJobsWithNoTestFailures = jobTestResults.entrySet().stream()
                .filter(entry -> CollectionUtils.isEmpty(entry.getValue()) && !entry.getKey().lastBuildWasSuccessful())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getKey().latestFailingTests(jenkinsConfig.maxJenkinsBuildsToCheck)));


        List<String> failingJobsWithNoTestFailuresHtml = failingJobsWithNoTestFailures.entrySet().stream()
                .map(entry -> createJobFragment(counter.getAndIncrement(), view.url, entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        view.failingJobsWithNoTestFailuresCount = failingJobsWithNoTestFailures.size();

        String resultsPage = createTestResultsHtmlPage(view, includeViewsLink, jobsResultsHtml, failingJobsWithNoTestFailuresHtml, jobsPassingHtml);

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
                                             List<String> failingJobsWithNoTestFailuresHtml, List<String> jobsPassingHtml) {
        String resultsPage;
        String footer = "";
        if (StringUtils.isNotBlank(jenkinsConfig.testMethodNameSearchUrl)) {
            String queryCharacter = jenkinsConfig.testMethodNameSearchUrl.contains("?") ? "&" : "?";
            String searchUrl = jenkinsConfig.testMethodNameSearchUrl + queryCharacter + "viewName=" + view.name;
            footer += "<p/>" + constructAnchorUsingNewTab(searchUrl, "Search saved results") + "<br/>";
        }
        if (includeViewsLink) {
            footer += "<p/><a href=\"index.html\">Back</a><br/>";
        }
        double passingRate = view.totalTestMethodsCount > 0 ? ((view.totalTestMethodsCount - view.failingTestMethodsCount) / (double) view.totalTestMethodsCount) * 100 : 0;
        footer +=  String.format("Generated at %s - %.2f%% pass rate (%,d tests failing out of %,d test methods)",
                generationDate, passingRate, view.failingTestMethodsCount, view.totalTestMethodsCount);
        if (consistentFailuresJobsHtml.isEmpty() && failingJobsWithNoTestFailuresHtml.isEmpty()) {
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

            resultsPage = resultsPage.replace("#failingJobsWithNoTestFailuresCount", String.valueOf(failingJobsWithNoTestFailuresHtml.size()));
            resultsPage = resultsPage.replace("#failingJobsWithNoTestFailures", String.join("\n", failingJobsWithNoTestFailuresHtml));
            resultsPage = resultsPage.replace("#passingJobsCount", String.valueOf(jobsPassingHtml.size()));
            resultsPage = resultsPage.replace("#passingJobs", String.join("\n", jobsPassingHtml));

            resultsPage = resultsPage.replace("#footer", footer);
            log.trace("Failures for view {}:\n{}", view.name, resultsPage);
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
            job.removeOldBuilds(jenkinsConfig.maxJenkinsBuildsToKeep);
        });

        jobsToCheckForFailingTests.forEach(job -> {
            List<TestResult> failingTests = job.createFailingTestsList(jenkinsConfig.maxJenkinsBuildsToCheck, jenkinsConfig.numberOfFailuresNeededToBeConsistentlyFailing);
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

        if (jenkinsConfig.refetchCount > jenkinsConfig.maxJenkinsBuildsToCheck) {
            log.info("refetchCount {} is greater than maxJenkinsBuildsToCheck {}, setting to same value",
                    jenkinsConfig.refetchCount, jenkinsConfig.maxJenkinsBuildsToCheck);
            jenkinsConfig.refetchCount = jenkinsConfig.maxJenkinsBuildsToCheck;
        }
        final int numberOfBuildsToCheck = builds.size() > jenkinsConfig.maxJenkinsBuildsToCheck ? jenkinsConfig.maxJenkinsBuildsToCheck : builds.size();
        final int lastBuildNumberToCheck = builds.get(numberOfBuildsToCheck - 1).buildNumber;
        final Integer lastBuildNumberToRefetch = jenkinsConfig.refetchCount > 0 ?
                builds.get(Math.min(builds.size(), jenkinsConfig.refetchCount) - 1).buildNumber : null;

        List<JobBuild> usableBuilds = builds.stream()
                .filter(build -> build.buildNumber >= lastBuildNumberToCheck
                        && ((lastBuildNumberToRefetch != null && build.buildNumber >= lastBuildNumberToRefetch)
                        || !job.buildIsTooOld(build.buildNumber, jenkinsConfig.maxJenkinsBuildsToCheck)))
                .collect(toList());

        job.usefulBuilds = usableBuilds.stream().parallel()
                .map(build -> jenkinsExecutor.execute(j -> j.getJobBuildDetails(build)))
                .peek(build -> build.setCommitIdForBuild(jenkinsConfig.commitIdInDescriptionPattern))
                .collect(toList());

        job.usefulBuilds.removeIf(build -> build.status == null);

        List<Supplier<TestResults>> usableResultSuppliers = job.usefulBuilds.stream()
                .filter(build -> build.status == SUCCESS || build.status == UNSTABLE || build.status == FAILURE)
                .map(build -> (Supplier<TestResults>) () -> jenkinsExecutor.execute(j -> j.getJobBuildTestResultsViaTestNGResultFiles(build)))
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
        long skippedTestCount = failingMethods.stream().filter(method -> method.status == TestResult.TestStatus.SKIP).count();
        long failedTestCount = failingMethods.stream().filter(method -> method.status != TestResult.TestStatus.SKIP).count();

        String filledInJobFragment = jobFragment.replace("#url", jobUrlWithViewName);
        filledInJobFragment = filledInJobFragment.replace("#runningBuildLink", fullDetails.runningBuildLink(viewUrl));
        filledInJobFragment = filledInJobFragment.replace("#latestBuildLink", fullDetails.latestBuildLink(viewUrl, jenkinsConfig.daysOldForShowingJobDate));
        if (skippedTestCount > 0) {
            filledInJobFragment = filledInJobFragment.replace("#jobLabel", fullDetails.name + " (" + pluralize(failedTestCount, "test")
                    + ", " + pluralize(skippedTestCount, "skip") + ")");
        } else {
            filledInJobFragment = filledInJobFragment.replace("#jobLabel", fullDetails.name + " (" + pluralize(failedTestCount, "test") + ")");
        }

        filledInJobFragment = filledInJobFragment.replace("#totalFailingCount", String.valueOf(failingMethods.size()));
        filledInJobFragment = filledInJobFragment.replace("#skippedTestCount", String.valueOf(skippedTestCount));
        filledInJobFragment = filledInJobFragment.replace("#failingTestCount", String.valueOf(failedTestCount));

        filledInJobFragment = filledInJobFragment.replace("#itemId", "item-" + jobIndex);

        StringBuilder rowBuilder = new StringBuilder();
        int index = 0;
        for (TestResult method : failingMethods.stream().sorted(comparingLong(TestResult::getStartedAt)
                .thenComparingInt(testResult -> testResult.executionOrder(jenkinsConfig.beforeConfigMethodPattern, jenkinsConfig.afterConfigMethodPattern))).collect(toList())) {
            String filledInRow = rowFragment.replace("#testName", method.fullTestNameForDisplay());
            filledInRow = filledInRow.replace("#testResultLinks", method.testLinks(viewUrl, jenkinsConfig.commitComparisonUrl));
            filledInRow = filledInRow.replace("#itemId", "item-" + jobIndex + "-" + index++);
            if (StringUtils.isNotBlank(method.exception)) {
                String exceptionText = method.exception.replace("\n", "<br/>");
                if (StringUtils.isNotBlank(jenkinsConfig.testIdPattern) && StringUtils.isNotBlank(jenkinsConfig.testLogIdUrlTemplate)) {
                    String logId = MatcherUtils.singleMatch(exceptionText, jenkinsConfig.testIdPattern);
                    if (StringUtils.isNotBlank(logId)) {
                        String logUrl = constructPartialLogUrl(jenkinsConfig.testLogIdUrlTemplate, method)
                                .replace("#logId", logId);
                        exceptionText = exceptionText.replaceFirst(logId, constructAnchorUsingNewTab(logUrl, logId));
                    }
                }
                if (StringUtils.isNotBlank(jenkinsConfig.testNameLogUrlTemplate)) {
                    String logUrl = constructPartialLogUrl(jenkinsConfig.testNameLogUrlTemplate, method)
                            .replace("#jobName", fullDetails.name.toLowerCase());
                    exceptionText += " " + constructAnchorUsingNewTab(logUrl, "Test logs");
                }

                filledInRow = filledInRow.replace("#exception", exceptionText);
            } else {
                filledInRow = filledInRow.replace("#exception", "");
            }

            rowBuilder.append(filledInRow).append("\n");
        }
        filledInJobFragment = filledInJobFragment.replace("#body", rowBuilder.toString());
        return filledInJobFragment;
    }

    private String constructPartialLogUrl(String template, TestResult method) {
        final long thirtySecondsInMillis = TimeUnit.SECONDS.toMillis(30);
        long startTimeMillis = method.startedAt > thirtySecondsInMillis ? method.startedAt - thirtySecondsInMillis : method.startedAt;
        long durationMillis = (long) (method.duration * 1000);
        long endTimeMillis = method.startedAt + durationMillis + thirtySecondsInMillis; // pad by thirty seconds both sides
        return template.replace("#testName", method.name)
                .replace("#buildNumber", String.valueOf(method.buildNumber))
                .replace("#startTimeMillis", String.valueOf(startTimeMillis))
                .replace("#endTimeMillis", String.valueOf(endTimeMillis));
    }

    private String constructAnchorUsingNewTab(String url, String text) {
        return "<a href = \"" + url + "\" " + LINK_IN_NEW_TAB + ">" + text + "</a>";
    }
}
