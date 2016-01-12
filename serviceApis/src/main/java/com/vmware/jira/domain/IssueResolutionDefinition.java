package com.vmware.jira.domain;

import com.vmware.util.complexenum.ComplexEnum;

public enum IssueResolutionDefinition implements ComplexEnum<Integer> {

    Fixed(1),
    WontFix(2),
    Duplicate(3),
    Incomplete(4),
    CannotReproduce(5),
    NotABug(6),
    Done(7),
    NeedInfo(8),
    Answered(9),
    AsDesigned(10),
    Verified(11),
    UnknownValue(-1);

    private int code;

    private IssueResolutionDefinition(int code) {
        this.code = code;
    }

    public Integer getValue() {
        return code;
    }
}
