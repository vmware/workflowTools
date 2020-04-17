package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.util.exception.FatalException;

import java.util.List;

@ActionDescription("Bulk create Jira issues.")
public class CreateIssues extends BaseBatchJiraAction {

    public CreateIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(projectIssues.getIssuesNotInJira().isEmpty(), "there are no issues not loaded from Jira");
    }

    @Override
    public void process() {
        List<Issue> issuesToCreate = projectIssues.getIssuesNotInJira();
        log.info("Creating {} issue[s]", issuesToCreate.size());

        Issue issueToBaseProjectAndComponentOff = null;

        for (Issue potentialIssueToCreate : issuesToCreate) {
            if (potentialIssueToCreate.fields.project == null) {
                if (issueToBaseProjectAndComponentOff == null) {
                    issueToBaseProjectAndComponentOff = getBaselineIssue();
                }

                potentialIssueToCreate.fields.project = issueToBaseProjectAndComponentOff.fields.project;
                potentialIssueToCreate.fields.components = issueToBaseProjectAndComponentOff.fields.components;
            }

            Issue createdIssue = jira.createIssue(potentialIssueToCreate);
            potentialIssueToCreate.setKey(createdIssue.getKey());
            potentialIssueToCreate.id = createdIssue.id;
            potentialIssueToCreate.self = createdIssue.self;
            log.info("Created issue with key {}, summary: {}",
                    createdIssue.getKey(), potentialIssueToCreate.getSummary());
        }
    }

    private Issue getBaselineIssue() {
        List<Issue> issuesFromJira = projectIssues.getIssuesFromJira();
        if (issuesFromJira.isEmpty()) {
            throw new FatalException("Expected to find issue in list that was already in Jira!");
        }
        Issue issueToUse = issuesFromJira.get(0);
        Issue baselineIssue = jira.getIssueByKey(issueToUse.getKey());

        String componentsText = baselineIssue.fields.getComponentsText();
        log.info("Selected first existing issue {} as baseline issue", baselineIssue.getKey());
        log.info("Derived project name {} and components {} from issue",
                baselineIssue.fields.project.name, componentsText);

        return baselineIssue;
    }
}
