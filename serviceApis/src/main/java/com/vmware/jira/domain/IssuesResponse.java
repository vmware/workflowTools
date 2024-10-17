package com.vmware.jira.domain;

public class IssuesResponse {

    public int total;

    public int startAt;
    public int maxResults;

    public Issue[] issues;

    public String[] warningMessages;
}
