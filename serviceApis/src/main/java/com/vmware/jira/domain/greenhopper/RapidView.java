package com.vmware.jira.domain.greenhopper;

import com.vmware.jira.domain.IssueTypeDefinition;

import java.util.ArrayList;
import java.util.List;

public class RapidView {

    public IssueSummary[] issues;

    public Sprint[] sprints;

    public List<IssueSummary> getStories(boolean includeStoriesInSprints) {
        List<IssueSummary> stories = new ArrayList<IssueSummary>();

        for (IssueSummary issue : this.issues) {
            if (issue.typeDefinition != IssueTypeDefinition.Story) {
                continue;
            }
            if (includeStoriesInSprints) {
                stories.add(issue);
            } else {
                addIfNotInSprint(stories, issue);
            }
        }
        return stories;
    }

    private void addIfNotInSprint(List<IssueSummary> issues, IssueSummary issue) {
        boolean found = false;
        for (Sprint sprint : sprints) {
            if (sprint.containsIssue(issue.id)) {
                found = true;
                break;
            }
        }
        if (!found) {
            issues.add(issue);
        }
    }


}
