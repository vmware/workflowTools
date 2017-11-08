package com.vmware.action.jira;

import com.vmware.action.base.BaseTransitionJiraIssue;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.IssueStatusDefinition;

import static com.vmware.jira.domain.IssueStatusDefinition.InProgress;
import static com.vmware.jira.domain.IssueStatusDefinition.InReview;
import static com.vmware.jira.domain.IssueStatusDefinition.Open;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;
import static com.vmware.jira.domain.IssueStatusDefinition.Resolved;

@ActionDescription("Marks the jira issue identified by the bug number as resolved if it has a status of open, reopened, in progress or in review.")
public class MarkIssueAsResolved extends BaseTransitionJiraIssue {

    public MarkIssueAsResolved(WorkflowConfig config) {
        super(config, new IssueStatusDefinition[] {Resolved},
                new IssueStatusDefinition[] {Open, Reopened, InProgress, InReview});
    }
}
