package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a git push origin HEAD:develop.")
public class PushToDevelop extends BaseAction {

    public PushToDevelop(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        git.pushToRemoteBranch(gitRepoConfig.defaultGitRemote, "develop");
    }
}
