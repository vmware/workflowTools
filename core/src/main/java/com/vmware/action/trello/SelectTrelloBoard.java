package com.vmware.action.trello;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.trello.domain.Board;
import com.vmware.util.input.InputUtils;

@ActionDescription("Selects an existing open trello board.")
public class SelectTrelloBoard  extends BaseTrelloAction {


    public SelectTrelloBoard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Board[] openBoards = trello.getOpenBoardsForUser();
        if (openBoards.length == 0) {
            log.info("No open boards available to select");
            return;
        }

        int boardToUse = InputUtils.readSelection(openBoards, "Trello Boards");
        selectedBoard = openBoards[boardToUse];

    }
}
