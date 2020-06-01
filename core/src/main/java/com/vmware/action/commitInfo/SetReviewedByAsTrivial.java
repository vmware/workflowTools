package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Sets reviewed by to be trivial.")
public class SetReviewedByAsTrivial extends BaseCommitWithReviewAction {
    public SetReviewedByAsTrivial(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Removing reviewers {}, setting reviewers as {}", draft.reviewedBy, commitConfig.trivialReviewerLabel);
        draft.reviewedBy = commitConfig.trivialReviewerLabel;
    }
}
