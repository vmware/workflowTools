package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if the current commit has no bug number or a bug number value of 'none'.")
public class ExitIfCommitHasNoBugNumber extends BaseCommitAction {

    public ExitIfCommitHasNoBugNumber(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (draft.hasBugNumber(commitConfig.noBugNumberLabel)) {
            return;
        }

        cancelWithMessage("commit has no bug number");
    }
}
