package com.vmware;

import com.google.gson.annotations.Expose;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

public class JobBuild {
    @Expose(serialize = false, deserialize = false)
    public String buildDisplayName;
    public int number;
    public String url;
    @Expose(serialize = false, deserialize = false)
    public BuildResult result;

    public JobBuild() {}

    public JobBuild(String buildDisplayName, String url, BuildResult result) {
        this.buildDisplayName = buildDisplayName;
        this.url = url;
        this.result = result;
    }

    public JobBuild(final int number, final String baseUrl, BuildResult result) {
        this.number = number;
        this.url = baseUrl + number + "/";
        this.result = result;
    }

    public JobBuild(final int number, final String baseUrl) {
        this(number, baseUrl, BuildResult.BUILDING);
    }

    public String getBaseUrl() {
        return url.substring(0, url.lastIndexOf(String.valueOf(number)));
    }

    public boolean containsUrl(String url) {
        return this.url != null && this.url.contains(url);
    }

    public String id() {
        return MatcherUtils.singleMatch(url, "http.+?/(\\d+)/*.*?");
    }

    public boolean isFailure() {
        return result == BuildResult.FAILURE || result == BuildResult.UNSTABLE;
    }

    public String getJenkinsInfoUrl() {
        return fullUrl("api/json");
    }

    public String getConsoleOutputUrl() {
        return fullUrl("consoleText");
    }

    public String getJenkinsStopUrl() {
        return fullUrl("stop");
    }

    public String details(boolean includeResult) {
        String buildName = buildDisplayName != null ? buildDisplayName : "Build";
        String buildInfo = buildName + " " + url;
        if (includeResult && result != null) {
            buildInfo += " " + result.name();
        }
        return buildInfo;
    }

    private String fullUrl(String path) {
        return UrlUtils.addTrailingSlash(url) + path;
    }
}
