package com.vmware.bugzilla.domain;

import com.vmware.rest.json.StringEnum;

/**
 * Valid resolution values for a bug.
 */
public enum BugResolutionType implements StringEnum {
    Fixed("fixed"),
    UserError("user error"),
    WontFix("wont fix"),
    UnableToDuplicate("unable to duplicate"),
    NotABug("not a bug"),
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
