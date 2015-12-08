package com.vmware.jira.domain;

import com.google.gson.annotations.SerializedName;

public class IssueResolution {

    protected IssueResolution() {}

    public IssueResolution(IssueResolutionDefinition definition) {
        this.definition = definition;
    }

    @SerializedName("id")
    public IssueResolutionDefinition definition;
}
