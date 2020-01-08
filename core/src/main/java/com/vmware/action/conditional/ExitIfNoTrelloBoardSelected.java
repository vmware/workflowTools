package com.vmware.action.conditional;

import com.vmware.action.trello.BaseTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exists if no trello board has been selected.")
public class ExitIfNoTrelloBoardSelected extends BaseTrelloAction {

    public ExitIfNoTrelloBoardSelected(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (selectedBoard == null) {
            cancelWithMessage("no trello board has been selected or created.");
        }
    }
}
