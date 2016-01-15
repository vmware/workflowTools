package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssuesResponse;

import java.util.Arrays;

@ActionDescription("Load closed issues assigned to the user that have no resolution.")
public class LoadClosedIssuesWithoutResolution extends BaseBatchJiraAction {

    private final static IssueResolutionDefinition NO_RESOLUTION = null;

    public LoadClosedIssuesWithoutResolution(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        IssuesResponse matchingIssues =
                jira.getIssuesForUser(config.username, IssueStatusDefinition.Closed, NO_RESOLUTION);
        if (matchingIssues.total == 0) {
            log.info("No issues retrieved for user {} that are closed with no resolution", config.username);
        } else {
            log.info("Retrieved {} issues that are closed with no resolution", matchingIssues.total);
            multiActionData.addAllIssues(Arrays.asList(matchingIssues.issues));
        }
    }
}
