package com.vmware.action.review;

import com.vmware.action.base.AbstractCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestStatus;
import com.vmware.reviewboard.domain.UserReview;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("For the associated review, adds a comment to review board that the review has been submitted. Leaves the review open for further reviews.")
public class SoftSubmitReview extends AbstractCommitWithReviewAction {
    public SoftSubmitReview(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        ReviewRequest reviewRequest = draft.reviewRequest;
        if (reviewRequest.status == ReviewRequestStatus.submitted) {
            log.info("Review request already marked as submitted");
            return;
        }

        UserReview softSubmitReview = reviewBoard.getSoftSubmitReview(reviewRequest);
        if (softSubmitReview != null) {
            log.info("Review request already commented as submitted \n({})", softSubmitReview);
            return;
        }

        String headRef = git.revParse("HEAD");

        log.info("Adding submitted comment to review request. Leaving open for further reviews.");

        UserReview review = new UserReview();
        review.body_top = String.format("Submitted as ref %s, left open for further reviews", headRef);
        review.isPublic = true;

        reviewBoard.createUserReview(reviewRequest, review);
        log.info("Successfully added review request submitted comment");
    }
}
