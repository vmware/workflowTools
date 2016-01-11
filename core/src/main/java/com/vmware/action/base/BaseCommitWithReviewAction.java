/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseCommitWithReviewAction extends BaseCommitAction {
    protected ReviewBoard reviewBoard;


    public BaseCommitWithReviewAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        reviewBoard = serviceLocator.getReviewBoard();
        if (draft != null && draft.reviewRequest == null) {
            draft.reviewRequest = reviewBoard.getReviewRequestById(draft.id);
        }
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
