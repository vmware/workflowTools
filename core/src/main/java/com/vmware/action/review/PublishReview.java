package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequest;

@ActionDescription("Publishes a review on the review board server.")
public class PublishReview extends BaseCommitUsingReviewBoardAction {

    public PublishReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        ReviewRequest reviewRequest = draft.reviewRequest;

        try {
            reviewBoard.getReviewRequestDraft(reviewRequest.getDraftLink());
        } catch (NotFoundException e) {
            log.error("No changes detected for review request {}\n{}", draft.id, reviewBoardConfig.reviewboardUrl + "/r/" + reviewRequest.id);
            return;
        }

        log.info("Publishing review request {}", reviewRequest.id);
        reviewBoard.publishReview(reviewRequest.getDraftLink(), commitConfig.reviewChangeDescription);
        reviewRequest.isPublic = true;
        log.info("Successfully published review request");
    }
}
