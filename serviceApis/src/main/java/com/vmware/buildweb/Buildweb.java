package com.vmware.buildweb;

import com.vmware.AbstractRestBuildService;
import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.buildweb.domain.BuildMachine;
import com.vmware.buildweb.domain.BuildMachines;
import com.vmware.buildweb.domain.BuildwebBuild;
import com.vmware.buildweb.domain.BuildwebId;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.logging.Padder;

import java.util.List;

import static com.vmware.util.UrlUtils.addRelativePaths;

/**
 * VMware specific build service.
 */
public class Buildweb extends AbstractRestBuildService {

    private final String buildwebUrl;
    private String buildwebLogFileName;

    public Buildweb(String buildwebUrl, String buildwebApiUrl, String buildwebLogFileName, String username) {
        super(buildwebApiUrl, "/", ApiAuthentication.none, username);
        this.buildwebUrl = buildwebUrl;
        this.buildwebLogFileName = buildwebLogFileName;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
    }

    public BuildwebBuild getSandboxBuild(String id) {
        String[] idParts = id.split("-");
        String buildType = idParts.length == 2 ? idParts[0] : "sb";
        String idForBuild = idParts.length == 2 ? idParts[1] : id;
        return connection.get(addRelativePaths(baseUrl, buildType, "build", idForBuild), BuildwebBuild.class);
    }

    public void logOutputForBuilds(ReviewRequestDraft draft, int linesToShow, BuildResult... results) {
        String urlToCheckFor = urlUsedInBuilds();
        log.debug("Displaying output for failed builds matching url {}", urlToCheckFor);
        List<JobBuild> jobsToCheck = draft.jobBuildsMatchingUrl(urlToCheckFor);
        jobsToCheck.stream().filter(jobBuild -> jobBuild.matches(results))
                .forEach(jobBuild -> {
                    Padder buildPadder = new Padder("Buildweb build {} result {}", jobBuild.id(), jobBuild.result);
                    buildPadder.infoTitle();
                    log.info(getBuildOutput(jobBuild.id(), linesToShow));
                    buildPadder.infoTitle();
                });
    }

    public String getBuildOutput(String buildId, int maxLinesToTail) {
        String logsUrl = getLogsUrl(buildId);
        return logsUrl != null ? IOUtils.tail(logsUrl, maxLinesToTail) : "";
    }

    public String getLogsUrl(String buildId) {
        BuildwebBuild build = getSandboxBuild(buildId);
        if (build.buildResult == BuildResult.STARTING) {
            return null;
        }
        BuildMachines machines = connection.get(addRelativePaths(baseUrl, build.buildMachinesUrl), BuildMachines.class);
        BuildMachine buildMachine = machines.realBuildMachine();
        String logsUrl;
        if (build.buildResult == BuildResult.BUILDING) {
            logsUrl = addRelativePaths("http://" + buildMachine.hostName, build.relativeBuildTreePath(), "logs", buildwebLogFileName);
        } else {
            logsUrl = addRelativePaths(build.buildTreeUrl, "logs", buildMachine.hostType, buildwebLogFileName);
        }
        return logsUrl;
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
        BuildwebId buildwebId = new BuildwebId(MatcherUtils.singleMatchExpected(url, "/(\\w\\w/\\d++)"));
        String buildApiUrl = baseUrl + buildwebId.buildApiPath();
        BuildwebBuild build = optimisticGet(buildApiUrl, BuildwebBuild.class);
        return build.buildResult;
    }

    @Override
    protected void updateAllBuildsResultSuccessValue(ReviewRequestDraft draft, boolean result) {
        draft.buildwebBuildsAreSuccessful = result;
    }
}
