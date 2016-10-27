package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.UrlUtils;

import java.util.List;

public class Job {
    public String name;
    public String url;
    public String color;

    @Expose(serialize = false, deserialize = false)
    public List<JobParameter> parameters;

    public Job() {}

    public Job(String url) {
        this.url = url;
    }

    public Job(String baseUrl, String jobName) {
        this.name = jobName;
        baseUrl = UrlUtils.addTrailingSlash(baseUrl);
        this.url = baseUrl + "job/" + jobName + "/";
    }

    public String getBuildUrl() {
        return url + "build";
    }

    public String getBuildWithParametersUrl() {
        return url + "buildWithParameters";
    }

    public String getInfoUrl() {
        return url + "api/json";
    }
}
