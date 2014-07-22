package com.vmware.jira.domain;

public class IssueTimeTracking {

    public String originalEstimate;

    private IssueTimeTracking() {

    }

    public IssueTimeTracking(String originalEstimate) {
        this.originalEstimate = originalEstimate;
    }
}
