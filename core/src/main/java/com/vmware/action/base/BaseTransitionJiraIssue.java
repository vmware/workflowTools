package com.vmware.action.base;

import com.vmware.IssueInfo;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueFields;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssueTransitions;
import com.vmware.jira.domain.JiraUser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public abstract class BaseTransitionJiraIssue extends BaseCommitAction {

    private final List<IssueStatusDefinition> allowedFromStatuses;

    private final IssueStatusDefinition[] toStatuses;
    private Jira jira;

    public BaseTransitionJiraIssue(WorkflowConfig config, IssueStatusDefinition[] toStatuses, IssueStatusDefinition[] allowedFromStatuses) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
        this.toStatuses = toStatuses;
        this.allowedFromStatuses = Arrays.asList(allowedFromStatuses);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (config.disableJira) {
            log.warn("Jira is disabled by config property disableJira");
            return false;
        }

        if (!draft.hasBugNumber(config.noBugNumberLabel)) {
            log.info("Skipping action {} as the commit has no bug number", this.getClass().getSimpleName());
            return false;
        }
        return super.canRunAction();
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.jira = serviceLocator.getAuthenticatedJira();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        String[] bugNumbers = draft.bugNumbersAsArray();
        for (String bugNumber : bugNumbers) {
            transitionIssue(bugNumber.trim());
        }

    }

    private void transitionIssue(String bugNumber) throws IOException, URISyntaxException, IllegalAccessException {
        IssueStatusDefinition lastStatusToTransitionTo = toStatuses[toStatuses.length - 1];

        if (config.parseBugzillaBugNumber(bugNumber) != null) {
            log.info("Bug number {} appears to be a bugzilla bug, can't transition to {}",
                    lastStatusToTransitionTo.name());
            return;
        }
        IssueInfo issue = draft.getIssueForBugNumber(bugNumber);
        if (issue != null && !(issue instanceof Issue)) {
            log.info("Issue is not a jira issue, can't transition issue of type {}", issue.getClass().getSimpleName());
            return;
        }
        if (issue == null || !issue.isReal()) {
            issue = jira.getIssueByKey(bugNumber);
        }
        IssueFields issueFields = ((Issue)issue).fields;

        JiraUser assignee = issueFields.assignee;

        if (assignee != null && !assignee.name.equals(config.username)) {
            log.info("Not transitioning issue {} as it is assigned to {}, current username is {}",
                    bugNumber, assignee.name, config.username);
            return;
        }

        if (issueFields.status.def.equals(lastStatusToTransitionTo)) {
            log.info("No need to transition jira issue {} to {} as it is already {}", bugNumber,
                    lastStatusToTransitionTo.name(), issueFields.status.def.name());
            return;
        }

        for (int i = 0; i < toStatuses.length; i ++) {
            transitionIssueToStatus(bugNumber, issueFields, toStatuses[i], i == toStatuses.length - 1);
        }
    }

    private void transitionIssueToStatus(String bugNumber, IssueFields issueFields, IssueStatusDefinition toStatus, boolean isLast) throws IOException, URISyntaxException, IllegalAccessException {

        IssueStatusDefinition currentStatus = issueFields.status.def;
        if (currentStatus.equals(toStatus)) {
            if (isLast) {
                log.info("No need to transition jira issue {} to {} as it is already {}", bugNumber, toStatus.name(), currentStatus.name());
            }
            return;
        } else if (!allowedFromStatuses.contains(currentStatus)) {
            log.info("Jira issue {} should not be transitioned from {} to {}", bugNumber, currentStatus.name(), toStatus.name());
            return;
        }

        IssueTransitions allowedTransitions = jira.getAllowedTransitions(bugNumber);
        if (allowedTransitions.canTransitionTo(toStatus)) {
            log.info("Transitioning jira issue {} from {} to {}", bugNumber, issueFields.status.name, toStatus.name());
            jira.transitionIssue(allowedTransitions.getTransitionForStatus(toStatus));
            log.info("Successfully transitioned jira issue to {}", toStatus.name());
            issueFields.status.def = toStatus;
        } else {
            log.info("Cannot transition jira issue {} from {} to {}", bugNumber, issueFields.status.name, toStatus.name());
        }
    }
}
