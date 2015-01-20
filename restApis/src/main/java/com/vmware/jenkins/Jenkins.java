package com.vmware.jenkins;

import com.vmware.AbstractRestService;
import com.vmware.jenkins.domain.*;
import com.vmware.rest.cookie.ApiAuthentication;
import com.vmware.rest.exception.InternalServerException;
import com.vmware.rest.exception.NotAuthorizedException;
import com.vmware.rest.request.RequestParam;
import com.vmware.rest.request.RequestHeader;
import com.vmware.rest.RestConnection;
import com.vmware.rest.UrlUtils;
import com.vmware.rest.credentials.UsernamePasswordAsker;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.rest.request.RequestBodyHandling;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jenkins extends AbstractRestService {

    private final boolean usesCsrf;
    private final boolean disableLogin;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String configureUrl;
    private JobsList jobsList = null;

    public Jenkins(String serverUrl, final String username, boolean usesCsrf, boolean disableLogin)
            throws IOException, URISyntaxException, IllegalAccessException {
        super(serverUrl, "api/json", ApiAuthentication.jenkins, username);
        this.configureUrl = baseUrl + "me/configure";
        this.usesCsrf = usesCsrf;
        this.disableLogin = disableLogin;
        connection = new RestConnection(RequestBodyHandling.AsUrlEncodedJsonEntity);

        String apiToken = readExistingApiToken();
        if (apiToken != null) {
            connection.setupBasicAuthHeader(new UsernamePasswordCredentials(username, apiToken));
        }
    }

    public JobsList getJobsListing() throws IOException, URISyntaxException {
        if (jobsList == null) {
            jobsList = connection.get(apiUrl, JobsList.class);
        }

        return jobsList;
    }

    public void invokeJob(Job jobToInvoke, JobParameters params) throws IOException, URISyntaxException, IllegalAccessException {
        optimisticPost(jobToInvoke.getBuildUrl(), params);
    }

    public JobDetails getJobDetails(Job jobToInvoke) throws IOException, URISyntaxException, IllegalAccessException {
        return optimisticGet(jobToInvoke.getInfoUrl(), JobDetails.class);
    }

    public JobBuildDetails getJobBuildDetails(JobBuild jobBuild) throws IOException, URISyntaxException, IllegalAccessException {
        return optimisticGet(jobBuild.getInfoUrl(), JobBuildDetails.class);
    }

    public void stopJobBuild(JobBuild jobBuildToStop) throws IllegalAccessException, IOException, URISyntaxException {
        optimisticPost(jobBuildToStop.getStopUrl(), null);
    }

    public void checkStatusOfJenkinsJobs(ReviewRequestDraft draft) throws IOException, URISyntaxException, IllegalAccessException {
        log.info("Checking status of jenkins jobs");
        boolean isSuccess = true;

        for (JobBuild jobBuild : draft.jobBuilds) {

            String jobUrl = jobBuild.url;
            String jobApiUrl = UrlUtils.addTrailingSlash(jobUrl) + "api/json";

            if (jobBuild.result == JobBuildResult.BUILDING) {
                try {
                    JobBuildDetails jobBuildDetails = this.getJobBuildDetails(jobApiUrl);
                    jobBuild.result = jobBuildDetails.building ? JobBuildResult.BUILDING : jobBuildDetails.result;
                    log.info("Job: {} Result: {}", jobUrl, jobBuild.result.name());
                } catch (NotFoundException nfe) {
                    log.info("Job {} did not return a job, job might still be in Jenkins queue", jobUrl);
                }
            } else {
                log.info("Job: {} Result: {}", jobUrl, jobBuild.result.name());
            }
            isSuccess = isSuccess && jobBuild.result == JobBuildResult.SUCCESS;
        }
        if (draft.jobBuilds.isEmpty()) {
            log.info("No jenkins jobs found in testing done text");
        }

        draft.jenkinsJobsAreSuccessful = isSuccess;
    }

    @Override
    protected void checkAuthenticationAgainstServer() throws IOException, URISyntaxException{
        if (disableLogin) {
            log.info("Login is disabled for jenkins");
            return;
        }
        String apiToken = scrapeUIForToken();
        saveApiToken(apiToken);
    }

    @Override
    protected void loginManually() throws IllegalAccessException, IOException, URISyntaxException {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(ApiAuthentication.jenkins);
        connection.setupBasicAuthHeader(credentials);
    }

    private JobBuildDetails getJobBuildDetails(String jobBuildUrl) throws IOException, URISyntaxException, IllegalAccessException {
        return optimisticGet(jobBuildUrl, JobBuildDetails.class);
    }

    @Override
    protected void optimisticPost(String url, Object param, RequestParam... params) throws IllegalAccessException, IOException, URISyntaxException {
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

    private String scrapeUIForToken() throws IOException, URISyntaxException {
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
