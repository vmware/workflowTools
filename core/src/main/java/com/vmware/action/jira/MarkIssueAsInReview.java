package com.vmware.action.jira;

import com.vmware.action.base.AbstractTransitionJiraIssue;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.IssueStatusDefinition;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.vmware.jira.domain.IssueStatusDefinition.InProgress;
import static com.vmware.jira.domain.IssueStatusDefinition.InReview;
import static com.vmware.jira.domain.IssueStatusDefinition.Open;
import static com.vmware.jira.domain.IssueStatusDefinition.Reopened;

@ActionDescription("Marks the jira issue identified by the bug number as in review if it has a status of open, reopened or in progress.")
public class MarkIssueAsInReview extends AbstractTransitionJiraIssue {

    public MarkIssueAsInReview(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config, new IssueStatusDefinition[] {InProgress, InReview},
                new IssueStatusDefinition[] {Open, Reopened, InProgress});
    }
}
