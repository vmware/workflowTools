package com.vmware.jira.domain;

import com.vmware.bugzilla.domain.Bug;
import com.vmware.utils.collections.UniqueArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents data needed across multiple actions.
 */
public class MultiActionData {

    public String projectName;

    private List<Issue> issuesForProcessing = new UniqueArrayList<>();

    private List<Bug> bugsForProcessing = new UniqueArrayList<>();

    public void reset() {
        projectName = "";
        issuesForProcessing.clear();
        bugsForProcessing.clear();
    }

    public List<Bug> getBugsForProcessing() {
        return Collections.unmodifiableList(bugsForProcessing);
    }

    public List<Issue> getIssuesFromJira() {
        List<Issue> issuesFromJira = new ArrayList<>();
        for (Issue issue : issuesForProcessing) {
            if (issue.isFromJira()) {
                issuesFromJira.add(issue);
            }
        }
        return Collections.unmodifiableList(issuesFromJira);
    }

    public List<Issue> getIssuesNotInJira() {
        List<Issue> issuesNotInJira = new ArrayList<>();
        for (Issue issue : issuesForProcessing) {
            if (!issue.isFromJira()) {
                issuesNotInJira.add(issue);
            }
        }
        return Collections.unmodifiableList(issuesNotInJira);
    }

    public List<Issue> getIssuesRepresentingBugzillaBugs(String bugzillaUrl) {
        List<Issue> issues = new ArrayList<Issue>();
        for (Issue issue : issuesForProcessing) {
            if (issue.matchingBugzillaNumber(bugzillaUrl) != null) {
                issues.add(issue);
            }
        }
        return Collections.unmodifiableList(issues);
    }

    public List<Issue> getIssuesForProcessing() {
        return Collections.unmodifiableList(issuesForProcessing);
    }

    public void add(Issue issue) {
        issuesForProcessing.add(issue);
    }

    public void add(Bug bug) {
        bugsForProcessing.add(bug);
    }

    public void addAllBugs(Collection<Bug> bugs) {
        bugsForProcessing.addAll(bugs);
    }

    public boolean noBugsAdded() {
        return bugsForProcessing.isEmpty();
    }

    public void addAllIssues(Collection<Issue> issues) {
        issuesForProcessing.addAll(issues);
    }

    public boolean noIssuesAdded() {
        return issuesForProcessing.isEmpty();
    }
}
