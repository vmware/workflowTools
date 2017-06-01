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
import com.vmware.jira.domain.IssueTypeDefinition;
import com.vmware.jira.domain.MenuItem;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.jira.domain.greenhopper.IssueSummary;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.util.exception.InvalidDataException;
import com.vmware.util.input.InputListSelection;
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
    public void process() {
        List<MenuItem> recentBoards = jira.getRecentBoardItems();
        if (recentBoards.isEmpty()) {
            log.info("No recent boards in jira. Please use the web UI to select the board you want to use");
            return;
        }

        log.info("Please select project board for loading backlog stories");
        log.info("Only recent boards for you are shown");

        Integer selection = InputUtils.readSelection(
                recentBoards.toArray(new InputListSelection[recentBoards.size()]), "Jira Boards");
        MenuItem selectedItem = recentBoards.get(selection);

        RapidView rapidView = jira.getRapidView(selectedItem.getBoardId());
        List<IssueTypeDefinition> typesToSearchFor = config.includeAllIssueTypes ? null : Arrays.asList(config.issueTypesToInclude);
        List<IssueSummary> backlogStories = rapidView.getIssues(typesToSearchFor, config.includeSprintStories);
        if (backlogStories.size() == 0) {
            log.info("No issues of type {} found for board {}", Arrays.toString(config.issueTypesToInclude), selectedItem.label);
            return;
        }

        projectIssues.reset();
        SearchRequest searchRequest = createSearchRequestForFullIssueInformation(backlogStories);
        if (searchRequest == null) {
            log.warn("No issues can be used as they have all been estimated and configuration does not include estimated stories");
            return;
        }
        List<Issue> issues = new ArrayList<>(Arrays.asList(jira.searchForIssues(searchRequest).issues));

        if (!config.includeStoriesWithEstimates) {
            removeIssuesWithStoryPoints(issues);
        }

        if (issues.isEmpty()) {
            log.warn("No issues can be used as they have all been estimated or have story points and configuration does not include estimated stories");
        }

        projectIssues.projectName = selectedItem.label;

        List<String> labels = getLabelsFromIssues(issues);
        List<String> fixByVersions = getFixByVersionsFromIssues(issues);


        if (config.useJiraLabel && labels.size() == 0) {
            throw new InvalidDataException("Use jira label is set to true but none of the issues have labels");
        }

        if (config.useFixVersion && fixByVersions.size() == 0) {
            throw new InvalidDataException("Use fix by version is set to true but none of the issues have fix by versions");
        }

        String additionalInfo = "";

        if (config.useJiraLabel) {
            log.info("Please enter label to use");
            int selectedLabelIndex = InputUtils.readSelection(labels, "Jira Labels");
            String selectedLabel = labels.get(selectedLabelIndex);
            issues = filterByLabel(issues, selectedLabel);
            additionalInfo = selectedLabel;
        }

        if (config.useFixVersion) {
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
        String jqlQuery = "";
        boolean atLeastOneStoryCanBeUsed = false;
        for (IssueSummary story : stories) {
            if (!config.includeStoriesWithEstimates && story.estimateStatistic.containsValidEstimate()) {
                log.debug("Skipping issue {} as it has already been estimated and configuration does not include estimated stories", story.key);
                continue;
            }
            atLeastOneStoryCanBeUsed = true;
            if (jqlQuery.isEmpty()) {
                jqlQuery = "id in (";
            } else {
                jqlQuery += ",";
            }

            jqlQuery += "'" + story.key + "'";
        }
        if (!atLeastOneStoryCanBeUsed) {
            return null;
        }
        jqlQuery += ")";
        return jqlQuery;
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
