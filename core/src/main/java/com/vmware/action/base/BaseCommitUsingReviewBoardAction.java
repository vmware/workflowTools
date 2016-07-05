/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseCommitUsingReviewBoardAction extends BaseCommitWithReviewAction {
    protected ReviewBoard reviewBoard;


    public BaseCommitUsingReviewBoardAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        reviewBoard = serviceLocator.getReviewBoard();
        if (draft != null && draft.reviewRequest == null) {
            draft.reviewRequest = reviewBoard.getReviewRequestById(draft.id);
        }
    }

    protected String determineSubmittedRef() {
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "ref " + git.revParse("head");
        }

        String currentChangelistId = serviceLocator.getPerforce().getCurrentChangelistId(draft.perforceChangelistId);
        return "changelist " + currentChangelistId;
    }

}
