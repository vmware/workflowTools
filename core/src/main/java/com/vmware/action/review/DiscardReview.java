package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestStatus;

@ActionDescription("Discards the review associated with the commit.")
public class DiscardReview extends BaseCommitUsingReviewBoardAction {

    public DiscardReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Discarding review request {}", draft.id);
        ReviewRequest reviewRequest = draft.reviewRequest;
        reviewRequest.status = ReviewRequestStatus.discarded;
        reviewBoard.updateReviewRequest(reviewRequest);
        log.info("Successfully discarded review request");
    }
}
