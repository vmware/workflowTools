package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithReviewAction extends BaseCommitAction {

    public BaseCommitWithReviewAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.isTrivialCommit(config.trivialReviewerLabel)) {
            return "commit is trivial";
        }

        if (!draft.hasReviewNumber()) {
            return "commit does not have a review url";
        }

        return super.cannotRunAction();
    }
}
