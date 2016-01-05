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

public abstract class AbstractCommitWithReviewAction extends AbstractCommitAction{
    protected ReviewBoard reviewBoard;


    public AbstractCommitWithReviewAction(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
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
    public boolean canRunAction() throws IOException, URISyntaxException {
        if (draft.isTrivialCommit(config.trivialReviewerLabel)) {
            log.info("Ignoring action {} as commit is trivial", this.getClass().getSimpleName());
            return false;
        }

        if (!draft.hasReviewNumber()) {
            log.info("Ignoring action {} as commit does not have a review url", this.getClass().getSimpleName());
            return false;
        }

        return true;
    }
}
