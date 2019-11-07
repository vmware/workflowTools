package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Exit if perforce client is not found.")
public class ExitIfPerforceUserNotLoggedIn extends BaseCommitAction {
    public ExitIfPerforceUserNotLoggedIn(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        boolean loggedIn = serviceLocator.getPerforce().isLoggedIn();
        if (!loggedIn) {
            exitWithMessage("perforce user is not logged in");
        }
    }
}
