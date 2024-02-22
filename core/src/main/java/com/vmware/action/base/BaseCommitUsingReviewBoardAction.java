/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.http.exception.InternalServerException;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

public abstract class BaseCommitUsingReviewBoardAction extends BaseCommitWithReviewAction {
    protected ReviewBoard reviewBoard;
    private final boolean failIfReviewBoardIsDown;

    public BaseCommitUsingReviewBoardAction(WorkflowConfig config) {
        this(config, true);
    }

    public BaseCommitUsingReviewBoardAction(WorkflowConfig config, boolean failIfReviewBoardIsDown) {
        super(config);
        this.failIfReviewBoardIsDown = failIfReviewBoardIsDown;
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void checkIfWorkflowShouldBeFailed() {
        super.checkIfWorkflowShouldBeFailed();
        if (failIfReviewBoardIsDown && serviceLocator.getReviewBoardException() != null) {
            throw new FatalException("Cannot continue as ReviewBoard is down", serviceLocator.getReviewBoardException());
        }
    }

    @Override
    public void preprocess() {
        if (serviceLocator.getReviewBoardException() != null) {
            return;
        }
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
        if (draft != null && StringUtils.isInteger(draft.id) && draft.reviewRequest == null) {
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
