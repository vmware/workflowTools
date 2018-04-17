package com.vmware.jira.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.vmware.bugzilla.domain.Bug;
import com.vmware.util.collection.OverwritableSet;

/**
 * Represents project issues
 */
public class ProjectIssues {

    public String projectName;

    public String boardId;

    private List<Issue> issuesForProcessing = new OverwritableSet.UniqueArrayList<>();

    private List<Bug> bugsForProcessing = new OverwritableSet.UniqueArrayList<>();



    public void reset() {
        projectName = "";
        boardId = "";
        clearIssues();
    }

    public void clearIssues() {
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
