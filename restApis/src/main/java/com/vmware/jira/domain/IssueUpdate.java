package com.vmware.jira.domain;

public class IssueUpdate {

    public IssueUpdate() {
        fields = new IssueFields();
    }

    public IssueUpdate(IssueTransition transition) {
        this();
        this.transition = transition;
    }

    public IssueUpdate(IssueTransition transition, IssueResolutionDefinition resolution) {
        this(transition);
        fields.resolution = new IssueResolution(resolution);
    }

    public IssueFields fields;

    public IssueTransition transition;

}
