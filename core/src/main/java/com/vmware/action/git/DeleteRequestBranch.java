package com.vmware.action.git;

import com.vmware.action.base.BaseCommitUsingGithubAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Deletes remote branch used for the pull / merge request.")
public class DeleteRequestBranch extends BaseCommitUsingGithubAction {

    public DeleteRequestBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranch = determineSourceMergeBranch();
        log.info("Deleting {}/{} branch used for request", gitRepoConfig.defaultGitRemote, remoteBranch);
        git.deleteRemoteBranch(gitRepoConfig.defaultGitRemote, remoteBranch);
    }
}
