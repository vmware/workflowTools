package com.vmware.action.conditional;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if the current commit has no bug number or a bug number value of 'none'.")
public class ExitIfCommitHasNoBugNumber extends AbstractCommitAction {

    public ExitIfCommitHasNoBugNumber(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (draft.hasBugNumber(config.noBugNumberLabel)) {
            return;
        }

        log.info("");
        log.info("Exiting as commit has no bug number");
        System.exit(0);
    }
}
