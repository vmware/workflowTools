package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BasePerforceCommitUsingGitAction extends BasePerforceCommitAction {
    public BasePerforceCommitUsingGitAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!git.workingDirectoryIsInGitRepo()) {
            return "not in git repo directory";
        }
        return super.cannotRunAction();
    }

}
