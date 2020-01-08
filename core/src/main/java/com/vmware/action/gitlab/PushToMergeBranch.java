package com.vmware.action.gitlab;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitUsingGitlabAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Performs a force git push to the remote branch used for the gitlab merge request.")
public class PushToMergeBranch extends BaseCommitUsingGitlabAction {

    public PushToMergeBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranch = determineSourceMergeBranch();
        log.info("Pushing to {}/{} for merge request", gitRepoConfig.defaultGitRemote, remoteBranch);

        git.forcePushToRemoteBranch(gitRepoConfig.defaultGitRemote, remoteBranch);
    }
}
