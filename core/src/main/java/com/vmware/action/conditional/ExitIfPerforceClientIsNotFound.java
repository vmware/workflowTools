package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Exit if perforce client name is not set.")
public class ExitIfPerforceClientIsNotFound extends BaseAction {
    public ExitIfPerforceClientIsNotFound(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isNotBlank(config.perforceClientName)) {
            return;
        }

        log.info("");
        log.info("Exiting as perforceClientName config value is not set, can also be set by git-p4.client git config value.");
        System.exit(0);
    }
}
