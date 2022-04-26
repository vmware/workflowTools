package com.vmware.jenkins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.JobView;
import com.vmware.jenkins.domain.TestResult;
import com.vmware.jenkins.domain.TestResults;
import com.vmware.jenkins.domain.User;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.logging.Padder;

import static com.vmware.http.cookie.ApiAuthentication.jenkins;

public class Jenkins extends AbstractRestBuildService {
    private final boolean usesCsrf;
    private final boolean disableLogin;

    private HomePage homePage = null;

    public Jenkins(String serverUrl, final String username, boolean usesCsrf, boolean disableLogin) {
        super(serverUrl, "api/json", jenkins, username);
        this.usesCsrf = usesCsrf;
        this.disableLogin = disableLogin;
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedFormEntity);

        if (disableLogin) {
            log.debug("Not attempting to read api token for Jenkins as disableLogin is true");
            return;
        }

        String apiToken = readExistingApiToken(credentialsType);
        if (apiToken != null) {
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
            TestResults results = get(jobBuild.getTestReportsApiUrl(), TestResults.class);
            results.setBuild(jobBuild);
            return results;
        } catch (NotFoundException nfe) {
            log.info("Failed to find test results for job build {}", jobBuild.name);
            return new TestResults(jobBuild);
        }
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
            return;
        }
        connection.get(UrlUtils.addRelativePaths(baseUrl, "me/api/json"), User.class);
    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(credentialsType);
        connection.setupBasicAuthHeader(credentials);
    }

    private JobBuild getJobBuildDetails(String jobBuildUrl) {
        return get(jobBuildUrl, JobBuild.class);
    }

    @Override
    protected <T> T post(String url, Class<T> responseConversionClass, Object param, RequestParam... params) {
        if (usesCsrf) {
            CsrfCrumb csrfCrumb = super.get(super.baseUrl + "crumbIssuer/api/json", CsrfCrumb.class);
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
