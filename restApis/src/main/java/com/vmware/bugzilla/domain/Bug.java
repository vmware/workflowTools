package com.vmware.bugzilla.domain;

import com.vmware.IssueInfo;
import com.vmware.utils.StringUtils;

import java.util.Map;

/**
 * Represents a bug in Bugzilla.
 */
public class Bug implements IssueInfo {

    private String key;

    private String summary;

    private String description;

    private boolean notFound;

    public Bug(String key) {
        this.key = key;
        this.notFound = true;
    }

    public Bug(Map values) {
        this.key =  String.valueOf(values.get("bug_id"));
        this.summary = (String) values.get("short_desc");
        this.description = (String) values.get("description");
        this.notFound = false;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isNotFound() {
        return notFound;
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

}
