package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.base.BaseCommitCreateAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git commit.")
public class Commit extends BaseCommitCreateAction {

    public Commit(WorkflowConfig config) {
        super(config);
    }
}
