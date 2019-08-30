package com.vmware.config;

import java.util.ArrayList;
import java.util.List;

import com.vmware.jira.domain.ProjectIssues;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.trello.domain.Board;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.VappData;

/**
 * Values that are persisted from workflow action to workflow action.
 * This is a bag of entities that is used to set persistent entities in the Workflow class.
 */
public class WorkflowActionValues {

    private ReviewRequestDraft draft;

    private ProjectIssues projectIssues;

    private Board trelloBoard;

    private VappData vappData;

    public WorkflowActionValues() {
        this.draft = new ReviewRequestDraft();
        this.projectIssues = new ProjectIssues();
        this.vappData = new VappData();
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

    public VappData getVappData() {
        return vappData;
    }
}
