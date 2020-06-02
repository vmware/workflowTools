/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

public abstract class BaseCommitUsingReviewBoardAction extends BaseCommitWithReviewAction {
    protected ReviewBoard reviewBoard;
    private RuntimeException reviewBoardException;

    public BaseCommitUsingReviewBoardAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        try {
            reviewBoard = serviceLocator.getReviewBoard();
        } catch (FatalException re) {
            this.reviewBoardException = re;
        }
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        if (reviewBoardException != null) {
            throw reviewBoardException;
        }
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
        if (draft != null && draft.id != null && draft.reviewRequest == null) {
            draft.reviewRequest = reviewBoard.getReviewRequestById(Integer.parseInt(draft.id));
            Repository repository = reviewBoard.getRepository(draft.reviewRequest.getRepositoryLink());
            draft.repoType = repository.getRepoType();
        }
    }

    protected String determineSubmittedDescription() {
        if (draft.hasMergeRequest()) {
            return "Merged via merge request " + draft.mergeRequestUrl;
        } else if (StringUtils.isEmpty(draft.perforceChangelistId)) {
            return "Submitted as ref " + git.revParse("head");
        } else {
            String currentChangelistId = serviceLocator.getPerforce().getCurrentChangelistId(draft.perforceChangelistId);
            return "Submitted as changelist " + currentChangelistId;
        }
    }

}
