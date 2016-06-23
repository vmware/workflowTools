package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;

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

        log.info("");
        log.info("Exiting as rbt is not installed.");
        System.exit(0);
    }
}
