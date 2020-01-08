package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitUsingGitlabAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Deletes remote branch used for the gitlab merge request.")
public class DeleteMergeBranch extends BaseCommitUsingGitlabAction {

    public DeleteMergeBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranch = determineSourceMergeBranch();
        log.info("Deleting {}/{} branch used for merge request", gitRepoConfig.defaultGitRemote, remoteBranch);

        git.deleteRemoteBranch(gitRepoConfig.defaultGitRemote, remoteBranch);
    }
}
