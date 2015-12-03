package com.vmware;

/**
 * Common Inteface for Jira issues and Bugzilla Issues
 */
public interface IssueInfo {

    public String getKey();

    public String getSummary();

    public boolean isNotFound();

    public boolean isReal();
}
