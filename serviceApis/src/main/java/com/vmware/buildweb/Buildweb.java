package com.vmware.buildweb;

import com.vmware.AbstractRestBuildService;
import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.buildweb.domain.SandboxBuild;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.logging.Padder;

import java.net.URL;
import java.util.List;

/**
 * VMware specific build service.
 */
public class Buildweb extends AbstractRestBuildService {

    private final String buildwebUrl;
    private String buildwebLogsUrlPattern;

    public Buildweb(String buildwebUrl, String buildwebApiUrl, String buildwebLogsUrlPattern, String username) {
        super(buildwebApiUrl, "/", ApiAuthentication.none, username);
        this.buildwebUrl = buildwebUrl;
        this.buildwebLogsUrlPattern = buildwebLogsUrlPattern;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
    }

    public SandboxBuild getSandboxBuild(String id) {
        return connection.get(baseUrl + "sb/build/" + id, SandboxBuild.class);
    }

    public void logOutputForFailedBuilds(ReviewRequestDraft draft, int linesToShow) {
        String urlToCheckFor = urlUsedInBuilds();
        log.debug("Displaying output for failed builds matching url {}", urlToCheckFor);
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(urlToCheckFor);
        jobsToCheck.stream().filter(jobBuild -> jobBuild.result == BuildResult.FAILURE)
                .forEach(jobBuild -> {
                    Padder buildPadder = new Padder("Buildweb build {} result", jobBuild.id());
                    buildPadder.errorTitle();
                    log.info(getBuildOutput(jobBuild.id(), linesToShow));
                    buildPadder.errorTitle();
                });
    }

    public String getBuildOutput(String buildNumber, int maxLinesToTail) {
        String logsUrl = String.format(buildwebLogsUrlPattern, buildNumber);
        return IOUtils.tail(logsUrl, maxLinesToTail);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        log.info("No need to authenticate against Buildweb");
    }

    @Override
    protected void loginManually() {
    }

    @Override
    protected String urlUsedInBuilds() {
        return buildwebUrl;
    }

    @Override
    protected BuildResult getResultForBuild(String url) {
        String buildNumber = MatcherUtils.singleMatchExpected(url, "/sb/(\\d++)");
        String buildApiUrl = baseUrl + "sb/build/" + buildNumber;
        SandboxBuild build = optimisticGet(buildApiUrl, SandboxBuild.class);
        return build.getBuildResult();
    }

    @Override
    protected void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result) {
        draft.buildwebBuildsAreSuccessful = result;
    }
}
