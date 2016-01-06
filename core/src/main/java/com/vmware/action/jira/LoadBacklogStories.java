/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.jira;

import com.vmware.action.base.AbstractBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.MenuItem;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.jira.domain.greenhopper.IssueSummary;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.utils.input.InputListSelection;
import com.vmware.utils.input.InputUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ActionDescription("Loads a list of jira stories for processing.")
public class LoadBacklogStories extends AbstractBatchJiraAction {

    public LoadBacklogStories(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
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
        List<IssueSummary> backlogStories = rapidView.getBacklogStories();
        if (backlogStories.size() == 0) {
            log.info("No backlog stories found for board {}", selectedItem.label);
            return;
        }

        multiActionData.reset();
        SearchRequest searchRequest = createSearchRequestForFullIssueInformation(backlogStories);

        Issue[] issues = jira.searchForIssues(searchRequest).issues;

        multiActionData.projectName = selectedItem.label;

        List<String> labels = getLabelsFromIssues(issues);

        if (config.useJiraLabel && labels.size() == 0) {
            throw new IllegalArgumentException("Use jira label is set to true but none of the issues have labels");
        }

        if (config.useJiraLabel) {
            log.info("Please enter label to use");
            int selectedLabelIndex = InputUtils.readSelection(labels, "Jira Labels");
            String selectedLabel = labels.get(selectedLabelIndex);
            multiActionData.addAllIssues(filterByLabel(issues, selectedLabel));
            multiActionData.projectName += " (" + selectedLabel + ")";
        } else {
            multiActionData.addAllIssues(Arrays.asList(issues));
        }

        List<Issue> issuesForProcessing = multiActionData.getIssuesForProcessing();
        log.debug("Added {} issues for processing", issuesForProcessing.size());
        log.debug("Added issues\n{}", issuesForProcessing.toString());
    }

    private SearchRequest createSearchRequestForFullIssueInformation(List<IssueSummary> backlogStories) {
        String jqlQuery = constructJqlQuery(backlogStories);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.jql = jqlQuery;
        searchRequest.maxResults = backlogStories.size();
        searchRequest.fields = new String[] {"summary","description","assignee","status","issuetype", "labels","customfield_10062","customfield_10100"};
        return searchRequest;
    }

    private String constructJqlQuery(List<IssueSummary> stories) {
        String jqlQuery = "";
        for (IssueSummary story : stories) {
            if (!config.includeStoriesWithEstimates && story.estimateStatistic.containsValue()) {
                log.debug("Skipping issue {} as it has already been estimated and configuration does not include estimated stories", story.key);
                continue;
            }

            if (jqlQuery.isEmpty()) {
                jqlQuery = "id in (";
            } else {
                jqlQuery += ",";
            }

            jqlQuery += "'" + story.key + "'";
        }
        jqlQuery += ")";
        return jqlQuery;
    }

    private List<String> getLabelsFromIssues(Issue[] issues) {
        Set<String> labels = new HashSet<String>();
        for (Issue issue : issues) {
            if (issue.fields.labels == null) {
                continue;
            }

            labels.addAll(Arrays.asList(issue.fields.labels));
        }

        return new ArrayList<String>(labels);
    }

    private List<Issue> filterByLabel(Issue[] issues, String label) {
        List<Issue> filteredIssues = new ArrayList<Issue>();

        for (Issue issue : issues) {
            if (issue.hasLabel(label)) {
                filteredIssues.add(issue);
            }
        }
        return filteredIssues;
    }

}
