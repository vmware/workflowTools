package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.github.Github;
import com.vmware.util.StringUtils;

public abstract class BaseCommitUsingGithubAction extends BaseCommitAction {
    protected Github github;

    public BaseCommitUsingGithubAction(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("githubUrl", "githubRepoOwnerName", "githubRepoName");
    }

    @Override
    public void asyncSetup() {
        super.asyncSetup();
        github = serviceLocator.getGithub();
    }
}
