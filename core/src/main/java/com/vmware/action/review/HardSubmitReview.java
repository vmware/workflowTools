package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestStatus;

@ActionDescription("Marks the associated review as submitted in review board.")
public class HardSubmitReview extends BaseCommitUsingReviewBoardAction {

    public HardSubmitReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        ReviewRequest reviewRequest = draft.reviewRequest;

        if (reviewRequest.status == ReviewRequestStatus.submitted) {
            log.info("Review request already marked as submitted");
            return;
        }

        log.debug("Marking review request as submitted");
        reviewRequest.status = ReviewRequestStatus.submitted;
        reviewRequest.description = determineSubmittedDescription();

        reviewBoard.updateReviewRequest(reviewRequest);
        log.info("Marked review request {} as submitted", reviewRequest.id);
    }
}
