package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestStatus;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Marks the associated review as submitted in review board.")
public class HardSubmitReview extends BaseCommitWithReviewAction {

    public HardSubmitReview(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        ReviewRequest reviewRequest = draft.reviewRequest;

        if (reviewRequest.status == ReviewRequestStatus.submitted) {
            log.info("Review request already marked as submitted");
            return;
        }

        String headRef = git.revParse("HEAD");

        log.debug("Marking review request as submitted");
        reviewRequest.status = ReviewRequestStatus.submitted;
        reviewRequest.description = "Submitted as ref " + headRef;

        reviewBoard.updateReviewRequest(reviewRequest);
        log.info("Successfully marked review request as submitted");
    }
}
