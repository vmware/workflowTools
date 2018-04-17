package com.vmware.jira.domain.greenhopper;

import com.google.gson.annotations.SerializedName;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.jira.domain.IssueStatus;
import com.vmware.jira.domain.IssueType;

public class IssueSummary {

    public int id;

    public String key;

    @SerializedName("typeId")
    public IssueTypeDefinition typeDefinition;

    public String typeName;

    public IssueStatus status;

    public boolean done;

    public String summary;

    public String assignee;

    public CustomField estimateStatistic;

    public IssueType getIssueType() {
        IssueType issueType = new IssueType(typeDefinition);
        issueType.name = typeName;
        return issueType;
    }
}
