package com.vmware;

import com.google.gson.annotations.Expose;
import com.vmware.util.MatcherUtils;

public class JobBuild {
    public int number;
    public String url;
    @Expose(serialize = false, deserialize = false)
    public BuildResult result;

    public JobBuild() {}

    public JobBuild(String url, BuildResult result) {
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

    public String getJenkinsInfoUrl() {
        return url + "api/json";
    }

    public String getJenkinsStopUrl() {
        return url + "stop";
    }

}
