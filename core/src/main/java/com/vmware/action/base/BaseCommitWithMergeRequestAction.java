package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithMergeRequestAction extends BaseCommitUsingGitlabAction {
    public BaseCommitWithMergeRequestAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        if (draft.gitlabMergeRequestId == null) {
            exitDueToFailureCheck("no git lab merge request id set");
        }
        super.failWorkflowIfConditionNotMet();
    }
}
