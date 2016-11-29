package com.vmware.jira.domain;

import com.vmware.util.complexenum.ComplexEnum;

public enum IssueStatusDefinition implements ComplexEnum<Integer> {

    Open(1),
    InProgress(3),
    Reopened(4),
    Resolved(5),
    Closed(6),
    InReview(10017),
    ReadyForQE(10020),
    Verified(10023),
    ResolvedButUnverified(10024),
    InPipeline(10128),
    UnknownValue(-1);


    private int code;

    private IssueStatusDefinition(final int code) {
        this.code = code;
    }

    public Integer getValue() {
        return code;
    }
}
