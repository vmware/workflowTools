package com.vmware.jenkins.domain;

public class Job {
    public String name;
    public String url;
    public String color;

    public Job() {}

    public Job(String url) {
        this.url = url;
    }

    public String getBuildUrl() {
        return url + "build";
    }

    public String getInfoUrl() {
        return url + "api/json";
    }
}
