package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.Gitlab;

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
}
