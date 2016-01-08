package com.vmware.action.conditional;

import com.vmware.action.trello.BaseTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Exists if no trello board has been selected.")
public class ExitIfNoTrelloBoardSelected extends BaseTrelloAction {

    public ExitIfNoTrelloBoardSelected(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (selectedBoard == null) {
            log.info("");
            log.info("Exiting as no trello board has been selected or created.");
            System.exit(0);
        }
    }
}
