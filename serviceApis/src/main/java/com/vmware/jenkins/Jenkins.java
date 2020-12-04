package com.vmware.jenkins;

import com.vmware.AbstractRestBuildService;
import com.vmware.BuildStatus;
import com.vmware.http.HttpConnection;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.InternalServerException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.jenkins.domain.CsrfCrumb;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.HomePage;
import com.vmware.jenkins.domain.TestResults;
import com.vmware.jenkins.domain.JobView;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.logging.Padder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.vmware.http.cookie.ApiAuthentication.jenkins;

public class Jenkins extends AbstractRestBuildService {

    private final boolean usesCsrf;
    private final boolean disableLogin;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String configureUrl;
    private HomePage homePage = null;

    public Jenkins(String serverUrl, final String username, boolean usesCsrf, boolean disableLogin) {
        super(serverUrl, "api/json", jenkins, username);
        this.configureUrl = baseUrl + "me/configure";
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
            homePage = connection.get(apiUrl, HomePage.class);
        }

        return homePage;
    }

    public void invokeJob(Job jobToInvoke) {
        optimisticPost(jobToInvoke.getBuildWithParametersUrl(), null);
    }

    public void invokeJobWithParameters(Job jobToInvoke, JobParameters params) {
        optimisticPost(jobToInvoke.getBuildWithParametersUrl(), params.toMap());
    }

    public JobView getFullViewDetails(String viewUrl) {
        return optimisticGet(UrlUtils.addRelativePaths(viewUrl, "api/json?depth=1"), JobView.class);
    }

    public Job getJobDetails(Job jobToInvoke) {
        return getJobDetails(jobToInvoke.getInfoUrl());
    }

    public Job getJobDetails(String url) {
        return optimisticGet(url, Job.class);
    }

    public JobBuild getJobBuildDetails(JobBuild jobBuild) {
        return optimisticGet(jobBuild.getJenkinsInfoUrl(), JobBuild.class);
    }

    public JobBuild getJobBuildDetails(String jobName, int buildNumber) {
        String jobUrl = UrlUtils.addRelativePaths(baseUrl, "job", jobName);
        return getJobBuildDetails(new JobBuild(buildNumber, jobUrl));
    }

    public TestResults getJobBuildTestResults(JobBuild jobBuild) {
        TestResults results = optimisticGet(jobBuild.getTestReportsApiUrl(), TestResults.class);
        results.setBuild(jobBuild);
        return results;
    }

    public void abortJobBuild(JobBuild jobBuildToAbort) {
        log.info("Aborting build {}", jobBuildToAbort.url);
        optimisticPost(jobBuildToAbort.stopUrl(), null);
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
                    String consoleOutput = IOUtils.tail(jobBuild.consoleUrl(), linesToShow);
                    log.info(consoleOutput);
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
        String apiToken = scrapeUIForToken();
        saveApiToken(apiToken, credentialsType);
    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(credentialsType);
        connection.setupBasicAuthHeader(credentials);
    }

    private JobBuild getJobBuildDetails(String jobBuildUrl) {
        return optimisticGet(jobBuildUrl, JobBuild.class);
    }

    @Override
    protected <T> T optimisticPost(String url, Class<T> responseConversionClass, Object param, RequestParam... params) {
        if (usesCsrf) {
            CsrfCrumb csrfCrumb = super.optimisticGet(super.baseUrl + "crumbIssuer/api/json", CsrfCrumb.class);
            RequestHeader csrfHeader = new RequestHeader(csrfCrumb.crumbRequestField, csrfCrumb.crumb);
            List<RequestParam> paramList = new ArrayList<>(Arrays.asList(params));
            paramList.add(csrfHeader);
            return super.optimisticPost(url, responseConversionClass, param, paramList.toArray(new RequestParam[0]));
        } else {
            return super.optimisticPost(url, responseConversionClass, param, params);
        }
    }

    private String scrapeUIForToken() {
        log.debug("Scraping {} for api token", configureUrl);
        try {
            String userConfigureWebPage = connection.get(configureUrl, String.class);
            Matcher tokenMatcher = Pattern.compile("name=\"_\\.apiToken\"\\s+value=\"(\\w+)\"").matcher(userConfigureWebPage);
            if (!tokenMatcher.find()) {
                return "";
            }
            return tokenMatcher.group(1);
        } catch (InternalServerException e) {
            if (e.getMessage().contains("AccessDeniedException")) {
                throw new NotAuthorizedException(e.getMessage());
            } else {
                throw e;
            }
        }
    }
}
