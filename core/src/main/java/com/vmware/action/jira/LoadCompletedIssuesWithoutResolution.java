package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssuesResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ActionDescription("Load closed issues assigned to the user that have no resolution.")
public class LoadCompletedIssuesWithoutResolution extends BaseBatchJiraAction {

    private final static IssueResolutionDefinition NO_RESOLUTION = null;

    public LoadCompletedIssuesWithoutResolution(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<Issue> matchingIssues = new ArrayList<>();
        addMatchingIssues(matchingIssues, IssueStatusDefinition.Closed);
        addMatchingIssues(matchingIssues, IssueStatusDefinition.Resolved);
        if (matchingIssues.isEmpty()) {
            log.info("No issues retrieved for user {} that are closed with no resolution", config.username);
        } else {
            log.info("Retrieved {} issues that are completed with no resolution", matchingIssues.size());
            projectIssues.addAllIssues(matchingIssues);
        }
    }

    private void addMatchingIssues(List<Issue> matchingIssues, IssueStatusDefinition status) {
        IssuesResponse returnedIssues = jira.getIssuesForUser(status, NO_RESOLUTION);
        if (returnedIssues.total > 0) {
            matchingIssues.addAll(Arrays.asList(returnedIssues.issues));
        }
    }
}
