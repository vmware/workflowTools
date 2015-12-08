package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;

public class JobBuild {
    public int number;
    public String url;
    @Expose(serialize = false, deserialize = false)
    public JobBuildResult result;

    public JobBuild() {}

    public JobBuild(String url, JobBuildResult result) {
        this.url = url;
        this.result = result;
    }

    public JobBuild(final int number, final String baseUrl, JobBuildResult result) {
        this.number = number;
        this.url = baseUrl + number + "/";
        this.result = result;
    }

    public JobBuild(final int number, final String baseUrl) {
        this(number, baseUrl, JobBuildResult.BUILDING);
    }

    public String getBaseUrl() {
        return url.substring(0, url.lastIndexOf(String.valueOf(number)));
    }

    public String getInfoUrl() {
        return url + "api/json";
    }

    public String getStopUrl() {
        return url + "stop";
    }

}
