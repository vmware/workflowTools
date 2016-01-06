package com.vmware.config;

import com.vmware.jira.domain.MultiActionData;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.trello.domain.Board;

/**
 * Values that are persisted from workflow action to workflow action.
 * This is a bag of entities that is used to set persistent entities in the Workflow class.
 */
public class WorkflowActionValues {

    private ReviewRequestDraft draft;

    private MultiActionData multiActionData;

    private Board trelloBoard;

    public WorkflowActionValues() {
        this.draft = new ReviewRequestDraft();
        this.multiActionData = new MultiActionData();
    }

    public ReviewRequestDraft getDraft() {
        return draft;
    }

    public MultiActionData getMultiActionData() {
        return multiActionData;
    }

    public Board getTrelloBoard() {
        return trelloBoard;
    }

    public void setTrelloBoard(Board trelloBoard) {
        this.trelloBoard = trelloBoard;
    }
}
