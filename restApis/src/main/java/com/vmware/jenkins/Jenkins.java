package com.vmware.jenkins;

import com.vmware.AbstractRestService;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobBuildDetails;
import com.vmware.jenkins.domain.JobBuildResult;
import com.vmware.jenkins.domain.JobDetails;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.JobsList;
import com.vmware.rest.ApiAuthentication;
import com.vmware.rest.RestConnection;
import com.vmware.rest.credentials.UsernamePasswordAsker;
import com.vmware.rest.credentials.UsernamePasswordCredentials;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.rest.request.RequestBodyHandling;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jenkins extends AbstractRestService {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private String configureUrl;
    private JobsList jobsList = null;

    public Jenkins(String serverUrl, final String username)
            throws IOException, URISyntaxException, IllegalAccessException {
        super(serverUrl, "api/json", ApiAuthentication.jenkins, username);
        this.configureUrl = baseUrl + "me/configure";
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

    private String scrapeUIForToken() throws IOException, URISyntaxException {
        log.debug("Scraping {} for api token", configureUrl);
        String userConfigureWebPage = connection.get(configureUrl, String.class);
        Matcher tokenMatcher = Pattern.compile("name=\"_\\.apiToken\"\\s+value=\"(\\w+)\"").matcher(userConfigureWebPage);
        if (!tokenMatcher.find()) {
            return "";
        }
        return tokenMatcher.group(1);
    }
}
