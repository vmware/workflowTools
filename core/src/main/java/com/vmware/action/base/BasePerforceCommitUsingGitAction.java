package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BasePerforceCommitUsingGitAction extends BasePerforceCommitAction {

    public BasePerforceCommitUsingGitAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(!git.workingDirectoryIsInGitRepo(), "not in git repo directory");
    }

}
