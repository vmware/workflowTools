package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.Gitlab;
import com.vmware.util.StringUtils;

public abstract class BaseCommitUsingGitlabAction extends BaseCommitAction {
    protected Gitlab gitlab;

    public BaseCommitUsingGitlabAction(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("gitlabUrl", "gitlabProjectId");
    }

    @Override
    public void asyncSetup() {
        super.asyncSetup();
        gitlab = serviceLocator.getGitlab();
    }

    protected String determineSourceMergeBranch() {
        if (StringUtils.isNotBlank(gitlabConfig.sourceMergeBranch)) {
            return gitlabConfig.sourceMergeBranch;
        } else {
            String sourceBranch = gitlabConfig.gitlabMergeBranchFormat.replace("$USERNAME", config.username);
            String branchName = git.currentBranch();
            sourceBranch = sourceBranch.replace("$BRANCH_NAME", branchName);
            return sourceBranch;
        }
    }

    protected String determineTargetMergeBranch() {
        if (StringUtils.isNotBlank(gitlabConfig.targetMergeBranch)) {
            return gitlabConfig.targetMergeBranch;
        } else {
            String trackingBranchPath = gitRepoConfig.trackingBranchPath();
            return trackingBranchPath.contains("/") ? trackingBranchPath.split("/") [1] : trackingBranchPath;
        }
    }
}
