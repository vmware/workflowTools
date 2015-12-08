package com.vmware.jira.domain.greenhopper;

import com.vmware.jira.domain.IssueTypeDefinition;

import java.util.ArrayList;
import java.util.List;

public class RapidView {

    public IssueSummary[] issues;

    public Sprint[] sprints;

    public List<IssueSummary> getBacklogStories() {
        List<IssueSummary> backlogIssues = new ArrayList<IssueSummary>();

        for (IssueSummary issue : issues) {
            if (issue.typeDefinition != IssueTypeDefinition.Story) {
                continue;
            }
            boolean found = false;
            for (Sprint sprint : sprints) {
                if (sprint.containsIssue(issue.id)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                backlogIssues.add(issue);
            }
        }
        return backlogIssues;
    }


}
