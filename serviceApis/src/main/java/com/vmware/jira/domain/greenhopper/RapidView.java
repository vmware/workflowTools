package com.vmware.jira.domain.greenhopper;

import com.vmware.jira.domain.IssueTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RapidView {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    public IssueSummary[] issues;

    public Sprint[] sprints;

    public List<IssueSummary> getIssues(List<IssueTypeDefinition> typesToSearchFor, boolean includeStoriesInSprints) {
        List<IssueSummary> stories = new ArrayList<IssueSummary>();

        for (IssueSummary issue : this.issues) {
            if (typesToSearchFor != null && !typesToSearchFor.contains(issue.typeDefinition)) {
                log.debug("Skipping issue {} as it is of type {}. Allowed types {}", issue.key, issue.typeDefinition, typesToSearchFor);
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
        boolean found = Arrays.stream(sprints).anyMatch(sprint -> sprint.containsIssue(issue.id));
        if (!found) {
            issues.add(issue);
        }
    }


}
