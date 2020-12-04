package com.vmware;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

import java.util.List;

/**
 * Superclass for common functionality for rest build services such as Jenkins and Buildweb.
 */
public abstract class AbstractRestBuildService extends AbstractRestService {

    protected AbstractRestBuildService(String baseUrl, String apiPath, ApiAuthentication credentialsType, String username) {
        super(baseUrl, apiPath, credentialsType, username);
    }

    public void checkStatusOfBuilds(ReviewRequestDraft draft) {
        String urlToCheckFor = urlUsedInBuilds();
        log.info("Checking status of builds matching url {}", urlToCheckFor);
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(urlToCheckFor);

        if (jobsToCheck.isEmpty()) {
            log.info("No builds found in testing done text");
            updateAllBuildsResultSuccessValue(draft, true);
            return;
        }

        updateAllBuildsResultSuccessValue(draft, checkIfAllBuildsSucceeded(jobsToCheck));
    }

    protected String urlUsedInBuilds() {
        return baseUrl;
    }

    private boolean checkIfAllBuildsSucceeded(List<JobBuild> buildsToCheck) {
        boolean isSuccess = true;
        for (JobBuild jobBuild : buildsToCheck) {
            String jobUrl = jobBuild.url;

            if (jobBuild.status == null
                    || jobBuild.status == BuildStatus.STARTING || jobBuild.status == BuildStatus.BUILDING) {
                try {
                    jobBuild.status = getResultForBuild(jobUrl);
                    log.info("{} {} Result: {}", jobBuild.name, jobUrl, jobBuild.status);
                } catch (NotFoundException nfe) {
                    log.info("{} {} could not be found", jobBuild.name, jobUrl);
                }
            } else {
                log.info("{} {} Result: {}", jobBuild.name, jobUrl, jobBuild.status);
            }
            isSuccess = isSuccess && jobBuild.status == BuildStatus.SUCCESS;
        }
        return isSuccess;
    }

    protected abstract BuildStatus getResultForBuild(String url);

    protected abstract void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result);
}
