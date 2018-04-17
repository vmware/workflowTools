package com.vmware.action.git;

import com.vmware.action.base.BaseCommitCreateAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a git commit.")
public class Commit extends BaseCommitCreateAction {

    public Commit(WorkflowConfig config) {
        super(config);
    }
}
