package com.vmware.action.trello;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

@ActionDescription("Creates a trello board. Uses the projectName if it is not blank. Otherwise asks user for name.")
public class CreateTrelloBoard  extends BaseTrelloAction {


    public CreateTrelloBoard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String boardName = projectIssues.projectName;

        if (StringUtils.isEmpty(boardName)) {
            boardName = InputUtils.readValueUntilNotBlank("Enter trello board name");
        }

        createTrelloBoard(boardName);
    }
}
