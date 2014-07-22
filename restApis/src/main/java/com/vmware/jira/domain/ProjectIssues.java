package com.vmware.jira.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectIssues {

    public String projectName;

    private List<Issue> issuesForProcessing = new ArrayList<Issue>();

    public void reset() {
        projectName = "";
        issuesForProcessing.clear();
    }

    public List<Issue> getIssuesFromJira() {
        List<Issue> issuesFromJira = new ArrayList<Issue>();
        for (Issue issue : issuesForProcessing) {
            if (issue.isFromJira()) {
                issuesFromJira.add(issue);
            }
        }
        return Collections.unmodifiableList(issuesFromJira);
    }

    public List<Issue> getIssuesNotInJira() {
        List<Issue> issuesNotInJira = new ArrayList<Issue>();
        for (Issue issue : issuesForProcessing) {
            if (!issue.isFromJira()) {
                issuesNotInJira.add(issue);
            }
        }
        return Collections.unmodifiableList(issuesNotInJira);
    }

    public List<Issue> getIssuesForProcessing() {
        return Collections.unmodifiableList(issuesForProcessing);
    }

    public void add(Issue issue) {
        issuesForProcessing.add(issue);
    }

    public void addAll(Collection<Issue> issues) {
        issuesForProcessing.addAll(issues);
    }

    public boolean isEmpty() {
        return issuesForProcessing.isEmpty();
    }
}
