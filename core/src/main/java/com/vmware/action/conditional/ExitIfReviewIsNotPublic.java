/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exits if a review request has not been made public.")
public class ExitIfReviewIsNotPublic extends BaseCommitUsingReviewBoardAction {

    public ExitIfReviewIsNotPublic(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (draft.reviewRequest.isPublic) {
            return;
        }

        cancelWithMessage("the review is not public");
    }
}
