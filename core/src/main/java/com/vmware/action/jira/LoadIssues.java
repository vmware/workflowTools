/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.jira.domain.FilterableIssueField;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.SearchRequest;
import com.vmware.jira.domain.greenhopper.IssueSummary;
import com.vmware.jira.domain.greenhopper.RapidView;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.jira.domain.FilterableIssueField.epic;
import static com.vmware.jira.domain.FilterableIssueField.fixByVersion;
import static com.vmware.jira.domain.FilterableIssueField.label;

@ActionDescription("Loads a list of jira issues for processing.")
public class LoadIssues extends BaseBatchJiraAction {

    public LoadIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(StringUtils.isEmpty(projectIssues.boardId),"no JIRA board selected");
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

        String additionalInfo = "";

        if (jiraConfig.useLabel) {
            List<String> labels = getFieldValuesFromIssues(issues, label);
            if (labels.isEmpty()) {
                throw new FatalException("Use label is set to true but none of the issues have labels");
            }

            log.info("Please enter labels to use");
            List<Integer> selectedIndices = InputUtils.readSelections(labels, "Jira Labels", false);
            List<String> selectedLabels = selectedIndices.stream().map(labels::get).collect(Collectors.toList());
            issues = filterByFieldValues(issues, label, selectedLabels);
            additionalInfo = StringUtils.join(selectedLabels);
        }

        if (jiraConfig.useFixVersion) {
            List<String> fixByVersions = getFieldValuesFromIssues(issues, fixByVersion);
            if (fixByVersions.isEmpty()) {
                throw new FatalException("Use fix by is set to true but none of the issues have fix by versions");
            }

            log.info("Please enter fix by versions to use");
            List<Integer> selectedIndices = InputUtils.readSelections(fixByVersions, "Jira Fix By Versions", false);
            List<String> selectedFixByVersions = selectedIndices.stream().map(fixByVersions::get).collect(Collectors.toList());
            issues = filterByFieldValues(issues, fixByVersion, selectedFixByVersions);
            additionalInfo += StringUtils.appendWithDelimiter(additionalInfo, fixByVersions, ",");
        }

        if (jiraConfig.useEpics) {
            List<String> epics = getFieldValuesFromIssues(issues, epic);
            if (epics.isEmpty()) {
                throw new FatalException("Use epics is set to true but none of the issues have parent epics");
            }

            log.info("Please enter parent epics to use");
            List<String> epicSummaries = epics.stream()
                    .map(epicKey -> epicKey + ": " + jira.getIssueByKey(epicKey).getSummary()).collect(Collectors.toList());
            List<Integer> selectedIndices = InputUtils.readSelections(epicSummaries, "Epics", false);
            List<String> selectedParentEpics = selectedIndices.stream().map(epics::get).collect(Collectors.toList());
            issues = filterByFieldValues(issues, epic, selectedParentEpics);
        }

        if (!additionalInfo.isEmpty()) {
            projectIssues.projectName += "(" + additionalInfo + ")";
        }
        projectIssues.addAllIssues(issues);

        List<Issue> issuesForProcessing = projectIssues.getIssuesForProcessing();
        log.debug("Added {} for processing", StringUtils.pluralize(issuesForProcessing.size(), "issue"));
        issuesForProcessing.forEach(issue -> log.debug(issue.toString()));
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
        List<String> fieldsToInclude = new ArrayList<>();
        fieldsToInclude.addAll(Arrays.asList("summary","description","assignee","status", "fixVersions","issuetype", "labels"));
        fieldsToInclude.addAll(jiraConfig.jiraCustomFieldNames.values());
        searchRequest.fields = fieldsToInclude.toArray(new String[fieldsToInclude.size()]);
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

    private List<String> getFieldValuesFromIssues(List<Issue> issues, FilterableIssueField fieldType) {
        return issues.stream()
                .flatMap(issue -> issue.fields.valuesForFilterableField(fieldType).stream()).distinct().collect(Collectors.toList());
    }

    private List<Issue> filterByFieldValues(List<Issue> issues, FilterableIssueField fieldType, List<String> values) {
        return issues.stream()
                .filter(issue -> values.stream()
                        .anyMatch(value -> issue.hasFieldValue(fieldType, value))).collect(Collectors.toList());
    }
}
