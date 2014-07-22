package com.vmware.jira.domain;

import com.google.gson.annotations.SerializedName;

public class IssueStatus {

    @SerializedName("id")
    public IssueStatusDefinition def;

    public String name;

    public String description;

    protected IssueStatus() {}

    public IssueStatus(IssueStatusDefinition def) {
        this.def = def;
    }
}
