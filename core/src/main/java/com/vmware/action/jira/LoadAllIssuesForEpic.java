package com.vmware.action.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueType;
import com.vmware.jira.domain.IssuesResponse;
import com.vmware.jira.domain.SearchRequest;

@ActionDescription("Loads all stories and subtasks for a specific epic id.")
public class LoadAllIssuesForEpic extends BaseBatchJiraAction {

    public LoadAllIssuesForEpic(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("epicId");
    }

    @Override
    public void process() {
        Issue epicIssue = jira.getIssueByKey(jiraConfig.epicId);
        IssueTypeDefinition definition = epicIssue.fields.issuetype.definition;
        if (definition != IssueTypeDefinition.Epic) {
            throw new RuntimeException("Issue " + jiraConfig.epicId + " is not an EPIC. It is of type " + definition.name());
        }
        log.info("Loading all stories and subtasks for Jira epic {}: {}", jiraConfig.epicId, epicIssue.getSummary());
        String project = jiraConfig.epicId.split("-")[0];
        IssuesResponse foundStories = search(String.format("project = %s AND \"EPIC Link\" = %s", project, jiraConfig.epicId));
        log.info("Found {} stories for epic {}", foundStories.total, foundStories.total);

        List<Issue> allIssuesForEpic = new ArrayList<>();

        if (foundStories.issues != null) {
            allIssuesForEpic.addAll(Arrays.asList(foundStories.issues));
            String storyKeys = Arrays.stream(foundStories.issues).map(Issue::getKey).collect(Collectors.joining(","));

            IssuesResponse allSubtasks = search("parent in (" + storyKeys + ")");

            /*List<Issue> allSubtasks = Arrays.stream(foundIssues.issues).limit(1).map(story -> {
                IssuesResponse foundSubtasks = search(String.format("project = %s AND parent = %s", project, story.getKey()));
                log.info(foundSubtasks.total + " results");
                return Arrays.asList(foundSubtasks.issues);
            }).flatMap(Collection::stream).collect(Collectors.toList());*/
            log.info("Found {} subtasks for epic {}", allSubtasks.total, jiraConfig.epicId);
            if (allSubtasks.issues != null) {
                allIssuesForEpic.addAll(Arrays.asList(allSubtasks.issues));
            }
        }

        projectIssues.addAllIssues(allIssuesForEpic);
    }

    private IssuesResponse search(String jql) {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.jql = jql;
        log.info("Running Jira search: {}", searchRequest.jql);
        List<String> fieldsToInclude = new ArrayList<>();
        fieldsToInclude.addAll(Arrays.asList("summary", "status", "fixVersions","issuetype", "labels", "parent"));
        fieldsToInclude.addAll(jiraConfig.jiraCustomFieldNames.values());
        searchRequest.fields = fieldsToInclude.toArray(new String[0]);
        searchRequest.maxResults = 1000;
        return jira.searchForIssues(searchRequest);
    }
}
