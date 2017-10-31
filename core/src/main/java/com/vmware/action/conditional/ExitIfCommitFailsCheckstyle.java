package com.vmware.action.conditional;

import com.vmware.action.base.BaseCheckstyleAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Runs checkstyle against files in commit. Does not run if config values are not set.")
public class ExitIfCommitFailsCheckstyle extends BaseCheckstyleAction {

    public ExitIfCommitFailsCheckstyle(WorkflowConfig config) {
        super(config, true);
    }
}
