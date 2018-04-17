package com.vmware;

import java.util.List;

import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

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

            if (jobBuild.result == null
                    || jobBuild.result == BuildResult.STARTING || jobBuild.result == BuildResult.BUILDING) {
                try {
                    jobBuild.result = getResultForBuild(jobUrl);
                    log.info("{}: {} Result: {}", jobBuild.buildDisplayName, jobUrl, jobBuild.result);
                } catch (NotFoundException nfe) {
                    log.info("{} {} could not be found", jobBuild.buildDisplayName, jobUrl);
                }
            } else {
                log.info("{}: {} Result: {}", jobBuild.buildDisplayName, jobUrl, jobBuild.result);
            }
            isSuccess = isSuccess && jobBuild.result == BuildResult.SUCCESS;
        }
        return isSuccess;
    }

    protected abstract BuildResult getResultForBuild(String url);

    protected abstract void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result);
}
