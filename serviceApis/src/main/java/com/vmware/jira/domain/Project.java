package com.vmware.jira.domain;

public class Project {

    public Integer id;

    public String key;

    public String name;

    private Project() {}

    public Project(String key) {
        this.key = key;
    }
}
