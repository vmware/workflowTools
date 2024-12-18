package com.vmware.action.base;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Common base class for github and gitlab actions")
public abstract class BaseCommitUsingGitServerAction extends BaseCommitAction {

    public BaseCommitUsingGitServerAction(WorkflowConfig config) {
        super(config);
    }

    protected String determineSourceMergeBranch() {
        if (StringUtils.isNotBlank(gitRepoConfig.sourceMergeBranch)) {
            return gitRepoConfig.sourceMergeBranch;
        } else {
            String sourceBranch = gitRepoConfig.gitMergeBranchFormat.replace("$USERNAME", config.username);
            String branchName = git.currentBranch();
            sourceBranch = sourceBranch.replace("$BRANCH_NAME", branchName);
            return sourceBranch;
        }
    }

    protected String determineTargetMergeBranch() {
        if (StringUtils.isNotBlank(gitRepoConfig.targetMergeBranch)) {
            return gitRepoConfig.targetMergeBranch;
        } else {
            String trackingBranchPath = gitRepoConfig.trackingBranchPath();
            return trackingBranchPath.contains("/") ? trackingBranchPath.split("/") [1] : trackingBranchPath;
        }
    }
}
