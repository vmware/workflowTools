package com.vmware.action.checkstyle;

import com.vmware.action.base.BaseCheckstyleAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Runs checkstyle against files in commit. Does not run if config values are not set.")
public class CheckCommitForCheckstyleErrors extends BaseCheckstyleAction {

    public CheckCommitForCheckstyleErrors(WorkflowConfig config) {
        super(config, false);
    }
}
