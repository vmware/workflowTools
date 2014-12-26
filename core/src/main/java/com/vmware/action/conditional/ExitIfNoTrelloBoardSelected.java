package com.vmware.action.conditional;

import com.vmware.action.trello.AbstractTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Exists if no trello board has been selected.")
public class ExitIfNoTrelloBoardSelected extends AbstractTrelloAction {

    public ExitIfNoTrelloBoardSelected(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (selectedBoard.hasNoId()) {
            log.info("");
            log.info("Exiting as no trello board has been selected or created.");
            System.exit(0);
        }
    }
}
