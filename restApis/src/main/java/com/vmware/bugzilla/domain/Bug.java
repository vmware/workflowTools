package com.vmware.bugzilla.domain;

import com.vmware.IssueInfo;
import com.vmware.utils.StringUtils;

/**
 * Represents a bug in Bugzilla.
 */
public class Bug implements IssueInfo {

    private String key;

    private String summary;

    public Bug(String key) {
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getSummary() {
        return "Bugzilla bug retrieval not yet supported";
    }

    @Override
    public boolean isNotFound() {
        return false;
    }

    @Override
    public boolean isReal() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof String) {
            String key = (String) o;
            return key.equals(this.key);
        }

        if (o == null || getClass() != o.getClass()) return false;

        Bug bug = (Bug) o;

        if (key != null ? !key.equals(bug.key) : bug.key != null) return false;

        return true;
    }

    public static Bug aBug(String prefix, String bugNumber) {
        int lengthToStrip = prefix.length();
        if (bugNumber.toUpperCase().startsWith(prefix.toUpperCase() + "-")) {
            lengthToStrip++;
        }

        String numberPart = bugNumber.substring(lengthToStrip);
        if (StringUtils.isInteger(numberPart)) {
            return new Bug(numberPart);
        } else {
            return new Bug(bugNumber);
        }
    }
}
