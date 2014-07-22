package com.vmware.action.jira;

import com.vmware.action.base.AbstractBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueType;
import com.vmware.jira.domain.IssueTypeDefinition;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Bulk create Jira stories that do not have a key")
public class CreateMissingStories extends AbstractBatchJiraAction {

    public CreateMissingStories(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (projectIssues.getIssuesNotInJira().isEmpty()) {
            log.info("Skipping {} as there are no issues loaded not from Jira.", this.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        List<Issue> issuesToCreate = projectIssues.getIssuesNotInJira();
        log.info("Creating {} issues", issuesToCreate.size());

        Issue issueToBaseProjectAndComponentOff = getBaselineIssue();

        String componentsText = issueToBaseProjectAndComponentOff.fields.getComponentsText();
        log.info("Selected first existing issue {} as baseline issue", issueToBaseProjectAndComponentOff.key);
        log.info("Derived project name {} and components {} from issue",
                issueToBaseProjectAndComponentOff.fields.project.name, componentsText);

        for (Issue potentialIssueToCreate : issuesToCreate) {
            potentialIssueToCreate.fields.issuetype = new IssueType(IssueTypeDefinition.Story);
            potentialIssueToCreate.fields.project = issueToBaseProjectAndComponentOff.fields.project;
            potentialIssueToCreate.fields.components = issueToBaseProjectAndComponentOff.fields.components;

            Issue createdIssue = jira.createIssue(potentialIssueToCreate);
            potentialIssueToCreate.key = createdIssue.key;
            potentialIssueToCreate.id = createdIssue.id;
            log.info("Created issue with key {} for new trello story {}",
                    createdIssue.key, potentialIssueToCreate.fields.summary);
        }
    }

    private Issue getBaselineIssue() throws IOException, URISyntaxException {
        List<Issue> issuesFromJira = projectIssues.getIssuesFromJira();
        if (issuesFromJira.isEmpty()) {
            throw new IllegalArgumentException("Expected to find issue in list that was already in Jira!");
        }
        Issue issueToUse = issuesFromJira.get(0);
        return jira.getIssueByKey(issueToUse.key);
    }
}
