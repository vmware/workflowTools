package com.vmware.action.trello;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.trello.domain.Board;
import com.vmware.utils.input.InputUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Selects an existing open trello board.")
public class SelectTrelloBoard  extends AbstractTrelloAction {


    public SelectTrelloBoard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Board[] openBoards = trello.getOpenBoardsForUser();
        if (openBoards.length == 0) {
            log.info("No open boards available to select");
            return;
        }

        int selectedBoard = InputUtils.readSelection(openBoards, "Trello Boards");
        this.selectedBoard = openBoards[selectedBoard];
    }
}
