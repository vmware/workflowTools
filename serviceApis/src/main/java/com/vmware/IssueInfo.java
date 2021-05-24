package com.vmware;

/**
 * Common Interface for Jira issues and Bugzilla Issues
 */
public interface IssueInfo {

    String getKey();

    String getLinkedBugNumber();

    String getWebUrl();

    String getSummary();

    String getDescription();

    boolean isNotFound();

    boolean isReal();
}
