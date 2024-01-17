package com.vmware.jenkins.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildStatus;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputListSelection;

public class JobBuild extends BaseDbClass implements InputListSelection {
    @SerializedName("fullDisplayName")
    public String name;

    @Expose(serialize = false, deserialize = false)
    public Long jobId;

    public String url;

    @SerializedName("number")
    public Integer buildNumber;

    @Expose(serialize = false, deserialize = false)
    public String commitId;

    @DbSaveIgnore
    public String description;

    @DbSaveIgnore
    public JobBuildAction[] actions;

    @DbSaveIgnore
    public JobBuildArtifact[] artifacts;

    @DbSaveIgnore
    public boolean building;

    @SerializedName("result")
    public BuildStatus status;

    @Expose(serialize = false, deserialize = false)
    public int failedCount;

    @Expose(serialize = false, deserialize = false)
    public int skippedCount;

    @SerializedName("timestamp")
    public long buildTimestamp;

    public Long duration;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    private boolean hasSavedTestResults;

    public JobBuild() {
    }

    public JobBuild(String buildDisplayName, String url, BuildStatus status) {
        this.name = buildDisplayName;
        this.url = url;
        this.status = status;
        String foundNumber = MatcherUtils.singleMatch(this.url, ".+/(\\d+)/*");
        this.buildNumber = foundNumber != null ? Integer.parseInt(foundNumber) : null;
    }

    public JobBuild(final int buildNumber, final String baseUrl, BuildStatus status) {
        this.buildNumber = buildNumber;
        this.url = UrlUtils.addTrailingSlash(baseUrl) + buildNumber;
        this.status = status;
    }

    public JobBuild(final int buildNumber, final String baseUrl) {
        this(buildNumber, baseUrl, BuildStatus.BUILDING);
    }

    public boolean containsUrl(String url) {
        return this.url != null && this.url.contains(url);
    }

    public boolean matches(BuildStatus... statuses) {
        return Arrays.stream(statuses).anyMatch(status -> status == this.status);
    }

    public String buildNumber() {
        return MatcherUtils.singleMatch(url, "http.+?/(\\d+)/*.*?");
    }

    public String details(boolean includeResult) {
        String buildName = name != null ? name : "Build";
        String buildInfo = buildName + " " + url;
        if (includeResult && status != null) {
            buildInfo += " " + status.name();
        }
        return buildInfo;
    }

    public void updateBuildNumber(int newBuildNumber) {
        if (buildNumber == null) {
            throw new FatalException("No build number found in url " + url);
        }
        this.url = this.url.replace(String.valueOf(buildNumber), String.valueOf(newBuildNumber));
        this.buildNumber = newBuildNumber;
        this.status = null;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public String consoleUrl() {
        return UrlUtils.addRelativePaths(url, "consoleFull");
    }

    public String logTextUrl() {
        return UrlUtils.addRelativePaths(url, "logText/progressiveText");
    }

    public String stopUrl() {
        return UrlUtils.addRelativePaths(url, "stop");
    }

    public String getJobBuildCommitId() {
        if (actions == null) {
            return null;
        }
        for (JobBuildAction action : actions) {
            if (action.lastBuiltRevision != null) {
                return action.lastBuiltRevision.SHA1;
            }
        }
        return null;
    }

    public String getJobInitiator() {
        return Arrays.stream(actions).filter(action -> action.causes != null).findFirst().map(action -> action.causes[0].userId)
                .orElseThrow(() -> new RuntimeException("Could not get username for job starter for job " + name));
    }

    public String getJenkinsInfoUrl() {
        return UrlUtils.addRelativePaths(url, "api/json");
    }

    public String getTestReportsUIUrl() {
        return UrlUtils.addRelativePaths(url, "testngreports");
    }

    public String getTestReportsApiUrl() {
        return UrlUtils.addRelativePaths(getTestReportsUIUrl(), "api/json?depth=3");
    }

    public String fullUrlForArtifact(JobBuildArtifact artifact) {
        return UrlUtils.addRelativePaths(url, "artifact", artifact.relativePath);
    }

    public BuildStatus realResult() {
        return building ? BuildStatus.BUILDING : status;
    }

    public boolean isNonFailureBuild() {
        return status == BuildStatus.SUCCESS || status == BuildStatus.UNSTABLE;
    }

    public boolean hasSavedTestResults() {
        return hasSavedTestResults;
    }

    public void setHasSavedTestResults(boolean hasSavedTestResults) {
        this.hasSavedTestResults = hasSavedTestResults;
    }

    public JobBuildArtifact getArtifactForPathPattern(String pathPattern) {
        return Arrays.stream(artifacts).filter(artifact -> artifact.matchesPathPattern(pathPattern)).findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find artifact for path pattern " + pathPattern + " for job " + name));
    }

    public List<JobBuildArtifact> getArtifactsForPathPattern(String pathPattern) {
        return Arrays.stream(artifacts).filter(artifact -> artifact.matchesPathPattern(pathPattern)).collect(Collectors.toList());
    }

    public String number() {
        return String.valueOf(buildNumber);
    }

    public void setCommitIdForBuild(String commitIdPattern) {
        commitId = MatcherUtils.singleMatch(description, commitIdPattern);
        if (StringUtils.isEmpty(commitId)) {
            commitId = getJobBuildCommitId();
        }
        if (StringUtils.isEmpty(commitId)) {
            this.commitId = "unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JobBuild that = (JobBuild) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String getLabel() {
        return name != null && name.endsWith(":") ? name.substring(0, name.length() - 1) : name;
    }
}
