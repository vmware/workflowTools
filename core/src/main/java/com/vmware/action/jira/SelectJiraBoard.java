package com.vmware.action.jira;

import com.vmware.action.base.BaseBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.MenuItem;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

import java.util.List;

@ActionDescription("Select a board on JIRA to use.")
public class SelectJiraBoard extends BaseBatchJiraAction {

    public SelectJiraBoard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<MenuItem> recentBoards = jira.getRecentBoardItems();
        if (recentBoards.isEmpty()) {
            log.info("No recent boards in jira. Please use the web UI to select the board you want to use");
            return;
        }
        projectIssues.reset();

        log.info("Please select project board for loading backlog stories");
        log.info("Only recent boards for you are shown");

        Integer selection = InputUtils.readSelection(
                recentBoards.toArray(new InputListSelection[recentBoards.size()]), "Jira Boards");
        MenuItem selectedItem = recentBoards.get(selection);
        projectIssues.boardId = selectedItem.getBoardId();
        projectIssues.projectName = selectedItem.getLabel();
    }
}
