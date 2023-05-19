package com.vmware.jira.domain;

public class AccessToken {
    public String name;

    public String rawToken;

    public AccessToken() {}

    public AccessToken(String name) {
        this.name = name;
    }
}
