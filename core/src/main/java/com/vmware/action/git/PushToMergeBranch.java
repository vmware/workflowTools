package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a force git push to the remote branch used for the merge or pull request.")
public class PushToMergeBranch extends BaseCommitAction {

    public PushToMergeBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranch = determineSourceMergeBranch();
        log.info("Pushing to {}/{} for request", gitRepoConfig.defaultGitRemote, remoteBranch);

        git.pushToRemoteBranch(gitRepoConfig.defaultGitRemote, remoteBranch, gitRepoConfig.forcePush);
    }
}
