package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.http.exception.NotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Bulk updates Jira issues.")
public class UpdateIssues extends BaseBatchJiraAction {

    public UpdateIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (multiActionData.getIssuesFromJira().isEmpty()) {
            return "there are no issues loaded from Jira";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        List<Issue> issuesFromJira = multiActionData.getIssuesFromJira();
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
