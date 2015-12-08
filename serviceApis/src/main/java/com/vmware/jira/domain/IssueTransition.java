package com.vmware.jira.domain;

import com.google.gson.annotations.Expose;

public class IssueTransition {

    @Expose(serialize = false, deserialize = false)
    public String issueId;

    public int id;

    @Expose(serialize = false)
    public IssueStatus to;
}
