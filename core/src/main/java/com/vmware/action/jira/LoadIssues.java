/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.FixVersion;
import com.vmware.jira.domain.Issue;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.jira.domain.greenhopper.IssueSummary;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@ActionDescription("Loads a list of jira issues for processing.")
public class LoadIssues extends BaseBatchJiraAction {

    public LoadIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isBlank(projectIssues.boardId)) {
            return "no JIRA board selected";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {

        RapidView rapidView = jira.getRapidView(projectIssues.boardId);
        List<IssueTypeDefinition> typesToSearchFor = jiraConfig.includeAllIssueTypes ? null : Arrays.asList(jiraConfig.issueTypesToInclude);
        List<IssueSummary> backlogStories = rapidView.getIssues(typesToSearchFor, jiraConfig.includeSprintStories);
        if (backlogStories.size() == 0) {
            log.info("No issues of type {} found for board {}", Arrays.toString(jiraConfig.issueTypesToInclude), projectIssues.projectName);
            return;
        }

        SearchRequest searchRequest = createSearchRequestForFullIssueInformation(backlogStories);
        if (searchRequest == null) {
            log.warn("No issues can be used as they have all been estimated and configuration does not include estimated stories");
            return;
        }
        List<Issue> issues = new ArrayList<>(Arrays.asList(jira.searchForIssues(searchRequest).issues));

        if (!jiraConfig.includeStoriesWithEstimates) {
            removeIssuesWithStoryPoints(issues);
        }

        if (issues.isEmpty()) {
            log.warn("No issues can be used as they have all been estimated or have story points and configuration does not include estimated stories");
        }

        List<String> labels = getLabelsFromIssues(issues);
        List<String> fixByVersions = getFixByVersionsFromIssues(issues);


        if (jiraConfig.useJiraLabel && labels.size() == 0) {
            throw new FatalException("Use jira label is set to true but none of the issues have labels");
        }

        if (jiraConfig.useFixVersion && fixByVersions.size() == 0) {
            throw new FatalException("Use fix by version is set to true but none of the issues have fix by versions");
        }

        String additionalInfo = "";

        if (jiraConfig.useJiraLabel) {
            log.info("Please enter label to use");
            int selectedLabelIndex = InputUtils.readSelection(labels, "Jira Labels");
            String selectedLabel = labels.get(selectedLabelIndex);
            issues = filterByLabel(issues, selectedLabel);
            additionalInfo = selectedLabel;
        }

        if (jiraConfig.useFixVersion) {
            log.info("Please enter fix by version to use");
            int selectedIndex = InputUtils.readSelection(fixByVersions, "Jira Fix By Versions");
            String selectedFixByVersion = fixByVersions.get(selectedIndex);
            issues = filterByFixByVersion(issues, selectedFixByVersion);
            if (!additionalInfo.isEmpty()) {
                additionalInfo += ",";
            }
            additionalInfo += selectedFixByVersion;
        }
        if (!additionalInfo.isEmpty()) {
            projectIssues.projectName += "(" + additionalInfo + ")";
        }
        projectIssues.addAllIssues(issues);

        List<Issue> issuesForProcessing = projectIssues.getIssuesForProcessing();
        log.debug("Added {} issues for processing", issuesForProcessing.size());
        log.debug("Added issues\n{}", issuesForProcessing.toString());
    }

    private void removeIssuesWithStoryPoints(List<Issue> issues) {
        Iterator<Issue> issueIterator = issues.iterator();
        while (issueIterator.hasNext()) {
            Issue issue = issueIterator.next();
            if (issue.fields.storyPoints != null) {
                log.debug("Skipping issue {} as it has story point value {} and configuration does not include estimated stories",
                        issue.getKey(), issue.fields.storyPoints);
                issueIterator.remove();
            }
        }
    }

    private SearchRequest createSearchRequestForFullIssueInformation(List<IssueSummary> backlogStories) {
        String jqlQuery = constructJqlQuery(backlogStories);
        if (jqlQuery == null) {
            return null;
        }

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.jql = jqlQuery;
        searchRequest.maxResults = backlogStories.size();
        searchRequest.fields = new String[] {"summary","description","assignee","status", "fixVersions","issuetype", "labels","customfield_10062","customfield_10100"};
        return searchRequest;
    }

    private String constructJqlQuery(List<IssueSummary> stories) {
        StringBuilder jqlQueryBuilder = new StringBuilder();
        boolean atLeastOneStoryCanBeUsed = false;
        for (IssueSummary story : stories) {
            if (!jiraConfig.includeStoriesWithEstimates && story.estimateStatistic.containsValidEstimate()) {
                log.debug("Skipping issue {} as it has already been estimated and configuration does not include estimated stories", story.key);
                continue;
            }
            atLeastOneStoryCanBeUsed = true;
            if (jqlQueryBuilder.length() == 0) {
                jqlQueryBuilder.append("id in (");
            } else {
                jqlQueryBuilder.append(",");
            }

            jqlQueryBuilder.append("'").append(story.key).append("'");
        }
        if (!atLeastOneStoryCanBeUsed) {
            return null;
        }
        jqlQueryBuilder.append(")");
        return jqlQueryBuilder.toString();
    }

    private List<String> getLabelsFromIssues(List<Issue> issues) {
        Set<String> labels = new HashSet<String>();
        for (Issue issue : issues) {
            if (issue.fields.labels == null) {
                continue;
            }

            labels.addAll(Arrays.asList(issue.fields.labels));
        }

        return new ArrayList<>(labels);
    }

    private List<String> getFixByVersionsFromIssues(List<Issue> issues) {
        Set<String> labels = new HashSet<String>();
        for (Issue issue : issues) {
            if (issue.fields.fixVersions == null) {
                continue;
            }
            for (FixVersion fixVersion : issue.fields.fixVersions) {
                labels.add(fixVersion.name);
            }
        }

        return new ArrayList<>(labels);
    }

    private List<Issue> filterByLabel(List<Issue> issues, String label) {
        List<Issue> filteredIssues = new ArrayList<Issue>();

        for (Issue issue : issues) {
            if (issue.hasLabel(label)) {
                filteredIssues.add(issue);
            }
        }
        return filteredIssues;
    }

    private List<Issue> filterByFixByVersion(List<Issue> issues, String version) {
        List<Issue> filteredIssues = new ArrayList<Issue>();

        for (Issue issue : issues) {
            if (issue.hasFixVersion(version)) {
                filteredIssues.add(issue);
            }
        }
        return filteredIssues;
    }

}
