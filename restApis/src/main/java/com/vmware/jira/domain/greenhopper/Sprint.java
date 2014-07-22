package com.vmware.jira.domain.greenhopper;

public class Sprint {

    public String id;

    public String name;

    public String state;

    public int[] issuesIds;

    public TimeRemaining timeRemaining;

    public boolean containsIssue(int issueIdToFind) {
        for (int issueId : issuesIds) {
            if (issueId == issueIdToFind) {
                return true;
            }
        }
        return false;
    }




}
