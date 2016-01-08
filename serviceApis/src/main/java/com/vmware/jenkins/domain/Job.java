package com.vmware.jenkins.domain;

import com.vmware.http.RequestParams;
import com.vmware.utils.UrlUtils;

public class Job {
    public String name;
    public String url;
    public String color;

    public Job() {}

    public Job(String url) {
        this.url = url;
    }

    public Job(String baseUrl, String jobName) {
        baseUrl = UrlUtils.addTrailingSlash(baseUrl);
        this.url = baseUrl + "job/" + jobName + "/";
    }

    public String getBuildUrl() {
        return url + "build";
    }

    public String getInfoUrl() {
        return url + "api/json";
    }
}
