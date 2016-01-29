package com.vmware.jenkins;

import com.vmware.AbstractRestService;
import com.vmware.jenkins.domain.*;
import com.vmware.http.HttpConnection;
import com.vmware.http.exception.InternalServerException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.http.cookie.ApiAuthentication.*;

public class Jenkins extends AbstractRestService {

    private final boolean usesCsrf;
    private final boolean disableLogin;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String configureUrl;
    private JobsList jobsList = null;

    public Jenkins(String serverUrl, final String username, boolean usesCsrf, boolean disableLogin)
            {
        super(serverUrl, "api/json", jenkins, username);
        this.configureUrl = baseUrl + "me/configure";
        this.usesCsrf = usesCsrf;
        this.disableLogin = disableLogin;
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedJsonEntity);

        String apiToken = readExistingApiToken(credentialsType);
        if (apiToken != null) {
            connection.setupBasicAuthHeader(new UsernamePasswordCredentials(username, apiToken));
        }
    }

    public JobsList getJobsListing() {
        if (jobsList == null) {
            jobsList = connection.get(apiUrl, JobsList.class);
        }

        return jobsList;
    }

    public void invokeJob(Job jobToInvoke, JobParameters params) {
        optimisticPost(jobToInvoke.getBuildUrl(), params);
    }

    public JobDetails getJobDetails(Job jobToInvoke) {
        return optimisticGet(jobToInvoke.getInfoUrl(), JobDetails.class);
    }

    public JobBuildDetails getJobBuildDetails(JobBuild jobBuild) {
        return optimisticGet(jobBuild.getInfoUrl(), JobBuildDetails.class);
    }

    public void stopJobBuild(JobBuild jobBuildToStop) {
        optimisticPost(jobBuildToStop.getStopUrl(), null);
    }

    public void checkStatusOfJenkinsJobs(ReviewRequestDraft draft) {
        log.info("Checking status of jenkins jobs");
        boolean isSuccess = true;

        for (JobBuild jobBuild : draft.jobBuilds) {

            String jobUrl = jobBuild.url;
            String jobApiUrl = UrlUtils.addTrailingSlash(jobUrl) + "api/json";

            if (jobBuild.result == JobBuildResult.BUILDING) {
                try {
                    JobBuildDetails jobBuildDetails = this.getJobBuildDetails(jobApiUrl);
                    jobBuild.result = jobBuildDetails.building ? JobBuildResult.BUILDING : jobBuildDetails.result;
                    log.info("Job: {} Result: {}", jobUrl, jobBuild.result);
                } catch (NotFoundException nfe) {
                    log.info("Job {} did not return a job, job might still be in Jenkins queue", jobUrl);
                }
            } else {
                log.info("Job: {} Result: {}", jobUrl, jobBuild.result);
            }
            isSuccess = isSuccess && jobBuild.result == JobBuildResult.SUCCESS;
        }
        if (draft.jobBuilds.isEmpty()) {
            log.info("No jenkins jobs found in testing done text");
        }

        draft.jenkinsJobsAreSuccessful = isSuccess;
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        if (disableLogin) {
            log.info("Login is disabled for jenkins");
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

    private JobBuildDetails getJobBuildDetails(String jobBuildUrl) {
        return optimisticGet(jobBuildUrl, JobBuildDetails.class);
    }

    @Override
    protected void optimisticPost(String url, Object param, RequestParam... params) {
        if (usesCsrf) {
            CsrfCrumb csrfCrumb = super.optimisticGet(super.baseUrl + "crumbIssuer/api/json", CsrfCrumb.class);
            RequestHeader csrfHeader = new RequestHeader(csrfCrumb.crumbRequestField, csrfCrumb.crumb);
            List<RequestParam> paramList = new ArrayList<RequestParam>(Arrays.asList(params));
            paramList.add(csrfHeader);
            super.optimisticPost(url, param, paramList.toArray(new RequestParam[paramList.size()]));
        } else {
            super.optimisticPost(url, param, params);
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
