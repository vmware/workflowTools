package com.vmware.config;

import com.vmware.jira.domain.ProjectIssues;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.trello.domain.Board;

/**
 * Values that are persisted from workflow action to workflow action.
 * This is a bag of entities that is used to set persistent entities in the Workflow class.
 */
public class WorkflowActionValues {

    private ReviewRequestDraft draft;

    private ProjectIssues projectIssues;

    private Board trelloBoard;

    public WorkflowActionValues() {
        this.draft = new ReviewRequestDraft();
        this.projectIssues = new ProjectIssues();
    }

    public ReviewRequestDraft getDraft() {
        return draft;
    }

    public ProjectIssues getProjectIssues() {
        return projectIssues;
    }

    public Board getTrelloBoard() {
        return trelloBoard;
    }

    public void setTrelloBoard(Board trelloBoard) {
        this.trelloBoard = trelloBoard;
    }
}
