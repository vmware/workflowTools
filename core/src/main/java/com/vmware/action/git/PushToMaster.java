package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git push origin HEAD:master.")
public class PushToMaster extends BaseAction {

    public PushToMaster(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        git.pushToRemoteBranch(config.defaultGitRemote, "master");
    }
}
