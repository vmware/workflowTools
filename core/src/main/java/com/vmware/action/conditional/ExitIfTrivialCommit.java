package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if the current commit is reviewed by trivial.")
public class ExitIfTrivialCommit extends BaseCommitAction {

    public ExitIfTrivialCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (!draft.isTrivialCommit(commitConfig.trivialReviewerLabel)) {
            return;
        }

        cancelWithMessage("the commit is trivial");
    }
}
