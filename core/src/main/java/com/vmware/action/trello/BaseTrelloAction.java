package com.vmware.action.trello;

import com.vmware.action.base.BaseIssuesProcessingAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.trello.Trello;
import com.vmware.trello.domain.Board;
import com.vmware.util.logging.Padder;

public abstract class BaseTrelloAction extends BaseIssuesProcessingAction {

    protected Trello trello;

    protected Board selectedBoard;

    public BaseTrelloAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        this.trello = serviceLocator.getTrello();
    }

    @Override
    public void preprocess() {
        this.trello.setupAuthenticatedConnection();
    }

    public void setSelectedBoard(Board selectedBoard) {
        this.selectedBoard = selectedBoard;
    }

    public Board getSelectedBoard() {
        return selectedBoard;
    }

    protected void createTrelloBoard(String boardName) {
        Board boardToCreate = new Board(boardName);

        Padder padder = new Padder("Trello Board {} creation", boardToCreate.name);
        padder.infoTitle();
        log.info("Creating trello board");

        Board createdBoard = trello.createBoard(boardToCreate);

        trello.createDefaultSwimlanesIfNeeded(createdBoard, trelloConfig.storyPointValues);
        padder.infoTitle();

        selectedBoard = createdBoard;
    }
}
