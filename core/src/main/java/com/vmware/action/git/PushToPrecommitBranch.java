package com.vmware.action.git;

import com.vmware.action.base.BaseCommitUsingGitRepoAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Performs a git push origin HEAD:topic/[username config property]/pre-commit.")
public class PushToPrecommitBranch extends BaseCommitUsingGitRepoAction {

    public PushToPrecommitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranchName = "precommit";
        String remoteBranchPath = gitRepoConfig.remoteBranches.get(remoteBranchName);
        if (StringUtils.isEmpty(remoteBranchPath)) {
            remoteBranchPath = "topic/$USERNAME/pre-commit";
        }

        remoteBranchPath = remoteBranchPath.replace("$USERNAME", config.username);

        git.pushToRemoteBranch(gitRepoConfig.defaultGitRemote, remoteBranchPath, gitRepoConfig.forcePush);
    }
}
