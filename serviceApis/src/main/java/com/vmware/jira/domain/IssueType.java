package com.vmware.jira.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.config.jira.IssueTypeDefinition;

public class IssueType {

    @SerializedName("id")
    public IssueTypeDefinition definition;

    public String name;

    public String description;

    public boolean subtask;

    public IssueType(IssueTypeDefinition definition) {
        this.definition = definition;
    }

}
