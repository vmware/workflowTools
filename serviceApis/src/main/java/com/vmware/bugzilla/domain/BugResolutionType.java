package com.vmware.bugzilla.domain;

import com.vmware.rest.json.StringEnum;

/**
 * Valid resolution values for a bug.
 * The enum names must match the IssueResolutionDefinition enum names so that equivalent resolutions can be mapped to each other.
 */
public enum BugResolutionType implements StringEnum {
    Fixed("fixed"),
    NotABug("user error"),
    WontFix("wont fix"),
    CannotReproduce("unable to duplicate"),
    AsDesigned("not a bug"),
    UnknownValue("Unknown value");

    private String value;

    BugResolutionType(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
