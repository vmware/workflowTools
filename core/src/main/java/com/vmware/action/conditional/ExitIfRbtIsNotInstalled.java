package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;

@ActionDescription("Exit if rbt is not installed.")
public class ExitIfRbtIsNotInstalled extends BaseAction {
    public ExitIfRbtIsNotInstalled(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (CommandLineUtils.isCommandAvailable("rbt")) {
            return;
        }

        cancelWithMessage("rbt is not installed.");
    }
}
