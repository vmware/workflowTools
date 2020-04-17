package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.exception.NotFoundException;
import com.vmware.jira.domain.Issue;

import java.util.List;

@ActionDescription("Bulk updates Jira issues.")
public class UpdateIssues extends BaseBatchJiraAction {

    public UpdateIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(projectIssues.getIssuesFromJira().isEmpty(), "there are no issues loaded from Jira");
    }

    @Override
    public void process() {
        List<Issue> issuesFromJira = projectIssues.getIssuesFromJira();
        log.info("Updating {} issues", issuesFromJira.size());

        for (Issue issueToUpdate : issuesFromJira) {
            try {
                jira.updateIssue(issueToUpdate);
                log.debug("Updated issue {}", issueToUpdate.getKey());
            } catch (NotFoundException e) {
                // ignore if the issue does not exist anymore in JIRA
                log.info("Ignoring missing issue '{}'", issueToUpdate.getKey());
            }
        }
    }

}
