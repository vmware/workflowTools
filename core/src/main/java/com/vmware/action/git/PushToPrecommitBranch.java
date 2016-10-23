package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git push origin HEAD:topic/[username config property]/pre-commit -f.")
public class PushToPrecommitBranch extends BaseAction {

    public PushToPrecommitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranchName = "precommit";
        String remoteBranchPath = config.remoteBranches.get(remoteBranchName);
        if (StringUtils.isBlank(remoteBranchPath)) {
            remoteBranchPath = "topic/$USERNAME/pre-commit";
        }

        remoteBranchPath = remoteBranchPath.replace("$USERNAME", config.username);

        git.forcePushToRemoteBranch(config.defaultGitRemote, remoteBranchPath);
    }
}
