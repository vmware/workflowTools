package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithReviewAction extends BaseCommitAction {

    public BaseCommitWithReviewAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.isTrivialCommit(commitConfig.trivialReviewerLabel), "commit is trivial");
        super.skipActionIfTrue(!draft.hasReviewNumber(), "commit does not have a review url");
    }
}
