package com.vmware.action.info;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;

@ActionDescription("Displays the commit message for the last commit on the console.")
public class DisplayLastCommit extends BaseCommitAction {

    public DisplayLastCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder titlePadder = new Padder("Last Commit");
        titlePadder.infoTitle();
        log.info(readLastChange());
        titlePadder.infoTitle();
    }
}
