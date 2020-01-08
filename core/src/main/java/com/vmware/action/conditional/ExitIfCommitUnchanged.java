package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if the commit details in memory are not different to the last commit.")
public class ExitIfCommitUnchanged extends BaseCommitAction {
    public ExitIfCommitUnchanged(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (!commitTextHasNoChanges(true)) {
            return;
        }

        cancelWithMessage("commit does not have any changes");
    }
}
