package com.vmware.jenkins;

import com.vmware.AbstractRestBuildService;
import com.vmware.BuildStatus;
import com.vmware.http.HttpConnection;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.jenkins.domain.CsrfCrumb;
import com.vmware.jenkins.domain.HomePage;
import com.vmware.jenkins.domain.JenkinsTestResults;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobBuildArtifact;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.JobView;
import com.vmware.jenkins.domain.TestNGXmlTestResults;
import com.vmware.jenkins.domain.TestResult;
import com.vmware.jenkins.domain.TestResults;
import com.vmware.jenkins.domain.User;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StopwatchUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.Padder;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.vmware.http.cookie.ApiAuthentication.jenkins;
import static com.vmware.util.StringUtils.humanReadableSize;

public class Jenkins extends AbstractRestBuildService {
    private final boolean usesCsrf;
    private String apiToken;
    private boolean disableLogin;
    private boolean apiTokenUsedForLogin;
    private Map<String, String> testReportUrlOverrides;

    private HomePage homePage = null;

    public Jenkins(String serverUrl, final String username, boolean usesCsrf, boolean disableLogin, Map<String, String> testReportUrlOverrides) {
        super(serverUrl, "api/json", jenkins, username);
        this.usesCsrf = usesCsrf;
        this.disableLogin = disableLogin;
        this.testReportUrlOverrides = testReportUrlOverrides;
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedFormEntity);
        apiToken = readExistingApiToken(credentialsType);

        if (!disableLogin && apiToken != null) {
            apiTokenUsedForLogin = true;
            connection.setupBasicAuthHeader(new UsernamePasswordCredentials(username, apiToken));
        }
    }

    public HomePage getHomePage() {
        if (homePage == null) {
            homePage = get(apiUrl, HomePage.class);
        }

        return homePage;
    }

    public void invokeJob(Job jobToInvoke) {
        post(jobToInvoke.getBuildWithParametersUrl(), null);
    }

    public void invokeJobWithParameters(Job jobToInvoke, JobParameters params) {
        post(jobToInvoke.getBuildWithParametersUrl(), params.toMap());
    }

    public JobView getFullViewDetails(String viewUrl) {
        return get(UrlUtils.addRelativePaths(viewUrl, "api/json?depth=1"), JobView.class);
    }

    public Job getJobDetails(Job jobToInvoke) {
        return get(jobToInvoke.getInfoUrl(), Job.class);
    }

    public JobBuild getJobBuildDetails(JobBuild jobBuild) {
        return get(jobBuild.getJenkinsInfoUrl(), JobBuild.class);
    }

    public JobBuild getJobBuildDetails(String jobName, int buildNumber) {
        String jobUrl = UrlUtils.addRelativePaths(baseUrl, "job", jobName);
        return getJobBuildDetails(new JobBuild(buildNumber, jobUrl));
    }

    public TestResults getJobBuildTestResults(JobBuild jobBuild) {
        try {
            JenkinsTestResults results = get(determineTestReportsApiUrl(jobBuild), JenkinsTestResults.class);
            results.setBuild(jobBuild);
            if (results.failConfig > 0) {
                addFailedConfigTestsViaJobHtmlPage(jobBuild, results);
            }
            return results;
        } catch (NotFoundException nfe) {
            log.info("Failed to find test results for job build {}", jobBuild.name);
            return new TestResults(jobBuild);
        }
    }

    public TestResults getJobBuildTestResultsViaTestNGResultFiles(JobBuild jobBuild) {
        try {
            TestResults allResults = new TestResults(jobBuild);
            List<JobBuildArtifact> testngResultsXmlFiles = jobBuild.getArtifactsForPathPattern(".+testng-results.xml");
            if (testngResultsXmlFiles.isEmpty()) {
                log.info("No testNG results files found for build {}, fetching via jenkins page", jobBuild.name);
                return getJobBuildTestResults(jobBuild);
            }
            for (JobBuildArtifact testngFile : testngResultsXmlFiles) {
                String artifactUrl = jobBuild.fullUrlForArtifact(testngFile);

                try {
                    TestResults results = getTestResults(jobBuild, artifactUrl);
                    allResults.combineTestResults(results);
                } catch (SAXException spe) {
                    log.error("Failed to parse artifact " + artifactUrl + " " + spe.getMessage() + ", retrying", spe);
                    try {
                        TestResults results = getTestResults(jobBuild, artifactUrl);
                        allResults.combineTestResults(results);
                    } catch (SAXException e) {
                        log.error("Failed again to parse artifact " + artifactUrl + " " + spe.getMessage(), spe);
                    }
                }
            }
            return allResults;
        } catch (NotFoundException nfe) {
            log.info("Failed to find test results for job build {}", jobBuild.name);
            return new TestResults(jobBuild);
        }
    }

    private TestResults getTestResults(JobBuild jobBuild, String artifactUrl) throws SAXException {
        log.debug("Fetching {}", artifactUrl);
        StopwatchUtils.Stopwatch stopwatch = StopwatchUtils.start();
        String fileContent = get(artifactUrl, String.class);
        if (stopwatch.elapsedTime(TimeUnit.SECONDS) >= 1) {
            log.info("Fetched {} ({}) in {} ms", artifactUrl, humanReadableSize(fileContent.getBytes().length), stopwatch.elapsedTime());
        } else {
            log.debug("Fetched {} ({}) in {} ms", artifactUrl, humanReadableSize(fileContent.getBytes().length), stopwatch.elapsedTime());
        }
        return new TestNGXmlTestResults(jobBuild, fileContent);
    }

    private void addFailedConfigTestsViaJobHtmlPage(JobBuild jobBuild, JenkinsTestResults results) {
        String testngReportsUrl = UrlUtils.addTrailingSlash(jobBuild.getTestReportsUIUrl());
        String jobHtmlPage = get(testngReportsUrl, String.class);
        if (StringUtils.isEmpty(jobHtmlPage)) {
            log.info("Failed to load {} when checking config tests", jobBuild.url);
            return;
        }
        String testFailurePattern = "<a href=\"(" + testngReportsUrl + ".+?)\">";
        List<String> failedTestUrls = MatcherUtils.allMatches(jobHtmlPage, testFailurePattern);
        if (failedTestUrls.isEmpty()) {
            log.info("No failed config methods found for {} using url {} and pattern {}",
                    jobBuild.name, jobBuild.url, testFailurePattern);
            return;
        }
        List<TestResult> loadedTestResults = results.testResults();
        failedTestUrls.removeIf(url -> loadedTestResults.stream().anyMatch(result -> url.equalsIgnoreCase(result.url)));
        if (failedTestUrls.isEmpty()) {
            log.info("No failed config methods found for {} using url {} and pattern {}",
                    jobBuild.name, jobBuild.url, testFailurePattern);
            return;
        }
        log.info("Found {} failed config methods for {}", failedTestUrls.size(), jobBuild.name);
        for (String url : failedTestUrls) {
            log.debug("Adding failed config method for url {}", url);
            TestResult failedConfigResult = get(UrlUtils.addRelativePaths(url, "api/json"), TestResult.class);
            failedConfigResult.url = url;
            failedConfigResult.configMethod = true;
            failedConfigResult.packagePath = MatcherUtils.singleMatchExpected(failedConfigResult.url, "testngreports/(.+?)/");
            failedConfigResult.buildNumber = jobBuild.buildNumber;
            failedConfigResult.commitId = jobBuild.commitId;
            results.addTestResult(failedConfigResult);
        }
    }

    private String determineTestReportsApiUrl(JobBuild jobBuild) {
        String testReportsApiUrl = jobBuild.getTestReportsApiUrl();
        if (jobBuild.artifacts != null && !testReportUrlOverrides.isEmpty()) {
            Optional<Map.Entry<String, String>> testUrlOverride = testReportUrlOverrides.entrySet().stream()
                    .filter(entry -> Arrays.stream(jobBuild.artifacts)
                            .anyMatch(artifact -> artifact.fileName.equals(entry.getKey()))).findFirst();
            if (testUrlOverride.isPresent()) {
                String testResultsRelativePath = testUrlOverride.get().getValue();
                testReportsApiUrl = UrlUtils.addRelativePaths(jobBuild.url, testResultsRelativePath);
            }
        }
        return testReportsApiUrl;
    }

    public void abortJobBuild(JobBuild jobBuildToAbort) {
        log.info("Aborting build {}", jobBuildToAbort.url);
        post(jobBuildToAbort.stopUrl(), null);
        jobBuildToAbort.status = BuildStatus.ABORTED;
    }

    public void logOutputForBuildsMatchingResult(ReviewRequestDraft draft, int linesToShow, BuildStatus... buildTypes) {
        String urlToCheckFor = urlUsedInBuilds();
        log.debug("Displaying output for builds matching url {} of type {}", urlToCheckFor, Arrays.toString(buildTypes));
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(urlToCheckFor);
        jobsToCheck.stream().filter(build -> build.matches(buildTypes))
                .forEach(jobBuild -> {
                    Padder buildPadder = new Padder("Jenkins build {} status {}", jobBuild.buildNumber(), jobBuild.status);
                    buildPadder.infoTitle();
                    if (jobBuild.status != BuildStatus.FAILURE && jobBuild.status != BuildStatus.UNSTABLE) {
                        String consoleOutput = IOUtils.tail(jobBuild.logTextUrl(), linesToShow);
                        log.info(consoleOutput);
                    } else {
                        List<TestResult> failedTests = getJobBuildTestResults(jobBuild).failedTestResults();
                        if (failedTests.isEmpty()) {
                            log.info("No failed tests found, showing last {} lines of log text", linesToShow);
                            String consoleOutput = IOUtils.tail(jobBuild.logTextUrl(), linesToShow);
                            log.info(consoleOutput);
                        } else {
                            List<String> failedTestsText = failedTests.stream().map(TestResult::fullTestNameWithExceptionInfo).collect(Collectors.toList());
                            IntStream.range(0, Math.min(linesToShow, failedTests.size())).forEach(index -> log.info(failedTestsText.get(index)));
                        }
                    }
                    buildPadder.infoTitle();
                });
    }

    public void abortAllRunningBuilds(ReviewRequestDraft draft) {
        String urlToCheckFor = urlUsedInBuilds();
        log.info("Aborting all builds matching url {}", urlToCheckFor);
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(urlToCheckFor);
        List<JobBuild> runningBuilds = jobsToCheck.stream()
                .filter(jobBuild -> jobBuild.status == BuildStatus.BUILDING).collect(Collectors.toList());
        if (runningBuilds.isEmpty()) {
            log.info("No builds running");
            return;
        }
        runningBuilds.forEach(this::abortJobBuild);
        draft.jenkinsBuildsAreSuccessful = false;
    }

    @Override
    protected BuildStatus getResultForBuild(String url) {
        String jobApiUrl = UrlUtils.addTrailingSlash(url) + "api/json";
        JobBuild buildDetails = this.getJobBuildDetails(jobApiUrl);
        return buildDetails.realResult();
    }

    @Override
    protected void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result) {
        draft.jenkinsBuildsAreSuccessful = result;
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        if (disableLogin) {
            log.debug("Login is disabled for jenkins");
        } else {
            connection.get(UrlUtils.addRelativePaths(baseUrl, "me/api/json"), User.class);
        }
    }

    @Override
    protected void loginManually() {
        if (disableLogin) {
            if (StringUtils.isNotBlank(apiToken) && !apiTokenUsedForLogin) {
                log.debug("Using api token as login is disabled");
            } else {
                apiToken = InputUtils.readValueUntilNotBlank("Api Token");
            }
            apiTokenUsedForLogin = true;
            connection.setupBasicAuthHeader(new UsernamePasswordCredentials(getUsername(), apiToken));
            connection.get(UrlUtils.addRelativePaths(baseUrl, "me/api/json"), User.class);
            saveApiToken(apiToken, jenkins);
        } else {
            UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(credentialsType, getUsername());
            connection.setupBasicAuthHeader(credentials);
            connection.get(UrlUtils.addRelativePaths(baseUrl, "me/api/json"), User.class);
        }

    }

    @Override
    protected void displayInputMessageForLoginRetry(int retryCount) {
        if (!disableLogin) {
            super.displayInputMessageForLoginRetry(retryCount);
        }
    }

    private JobBuild getJobBuildDetails(String jobBuildUrl) {
        return get(jobBuildUrl, JobBuild.class);
    }

    @Override
    protected <T> T post(String url, Class<T> responseConversionClass, Object param, RequestParam... params) {
        if (usesCsrf) {
            CsrfCrumb csrfCrumb = super.get(UrlUtils.addRelativePaths(super.baseUrl, "crumbIssuer/api/json"), CsrfCrumb.class);
            RequestHeader csrfHeader = new RequestHeader(csrfCrumb.crumbRequestField, csrfCrumb.crumb);
            List<RequestParam> paramList = new ArrayList<>(Arrays.asList(params));
            paramList.add(csrfHeader);
            connection.setUseSessionCookies(true);
            return super.post(url, responseConversionClass, param, paramList.toArray(new RequestParam[0]));
        } else {
            return super.post(url, responseConversionClass, param, params);
        }
    }
}
