package com.vmware.action.trello;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.trello.domain.Board;
import com.vmware.trello.domain.Swimlane;
import com.vmware.utils.InputUtils;
import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@ActionDescription("Creates a trello board. Uses the projectName if it is not blank. Otherwise asks user for name")
public class CreateTrelloBoard  extends AbstractTrelloAction {


    public CreateTrelloBoard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        String boardName = projectIssues.projectName;

        if (StringUtils.isBlank(boardName)) {
            boardName = InputUtils.readValueUntilNotBlank("Enter trello board name");
        }

        createTrelloBoard(boardName);
    }
}
