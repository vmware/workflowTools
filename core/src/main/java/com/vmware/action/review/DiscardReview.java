package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestStatus;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Discards the review associated with the commit.")
public class DiscardReview extends BaseCommitWithReviewAction {

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
