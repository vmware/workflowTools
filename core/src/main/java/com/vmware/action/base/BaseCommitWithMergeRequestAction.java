package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithMergeRequestAction extends BaseCommitUsingGitlabAction {
    public BaseCommitWithMergeRequestAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(!draft.mergeRequestLoaded(), "no git lab merge request loaded");
    }
}
