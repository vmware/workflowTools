package com.vmware;

import com.google.gson.annotations.Expose;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;

import java.util.Arrays;

public class JobBuild {
    @Expose(serialize = false, deserialize = false)
    public String buildDisplayName;
    public Integer number;
    public String url;
    @Expose(serialize = false, deserialize = false)
    public BuildResult result;

    public JobBuild() {}

    public JobBuild(String buildDisplayName, String url, BuildResult result) {
        this.buildDisplayName = buildDisplayName;
        this.url = url;
        this.result = result;
        String foundNumber = MatcherUtils.singleMatch(this.url, ".+/(\\d+)/*");
        this.number = foundNumber != null ? Integer.parseInt(foundNumber) : null;
    }

    public JobBuild(final int number, final String baseUrl, BuildResult result) {
        this.number = number;
        this.url = baseUrl + number;
        this.result = result;
    }

    public JobBuild(final int number, final String baseUrl) {
        this(number, baseUrl, BuildResult.BUILDING);
    }

    public boolean containsUrl(String url) {
        return this.url != null && this.url.contains(url);
    }

    public boolean matches(BuildResult... results) {
        return Arrays.stream(results).anyMatch(result -> result == this.result);
    }

    public String id() {
        return MatcherUtils.singleMatch(url, "http.+?/(\\d+)/*.*?");
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

    public void updateBuildNumber(int newBuildNumber) {
        if (number == null) {
            throw new FatalException("No build number found in url " + url);
        }
        this.url = this.url.replace(String.valueOf(number), String.valueOf(newBuildNumber));
        this.number = newBuildNumber;
        this.result = null;
    }

    private String fullUrl(String path) {
        return UrlUtils.addTrailingSlash(url) + path;
    }

    public Integer getNumber() {
        return number;
    }
}
