package com.vmware.action.trello;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractBatchIssuesAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.trello.Trello;
import com.vmware.trello.domain.Board;
import com.vmware.trello.domain.Swimlane;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class AbstractTrelloAction extends AbstractBatchIssuesAction {

    protected Trello trello;

    protected Board selectedBoard;

    public AbstractTrelloAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.trello = ServiceLocator.getTrello(config.trelloUrl);
    }

    public void setSelectedBoard(Board selectedBoard) {
        this.selectedBoard = selectedBoard;
    }

    public Board getSelectedBoard() {
        return selectedBoard;
    }

    protected void createTrelloBoard(String boardName) throws IllegalAccessException, IOException, URISyntaxException {
        Board boardToCreate = new Board(boardName);

        Padder padder = new Padder("Trello Board {} creation", boardToCreate.name);
        padder.infoTitle();
        log.info("Creating trello board");

        Board createdBoard = trello.createBoard(boardToCreate);
        Swimlane[] swimlanes = trello.getSwimlanesForBoard(createdBoard);

        log.info("Closing all existing lanes except for todo lane");
        for (int i = 1; i < swimlanes.length; i ++) {
            log.info("Closing unneeded board {}", swimlanes[i].name);
            trello.closeSwimlane(swimlanes[i]);
        }

        for (Integer storyPointValue : config.storyPointValues) {
            Swimlane swimlaneToCreate = new Swimlane(createdBoard, storyPointValue + Swimlane.STORY_POINTS_SUFFIX);
            log.info("Creating swimlane {}", swimlaneToCreate.name);
            trello.createSwimlane(swimlaneToCreate);
        }

        log.info("Creating parking lot lane");
        trello.createSwimlane(new Swimlane(createdBoard, "Parking Lot"));
        padder.infoTitle();

        selectedBoard = createdBoard;
    }
}
