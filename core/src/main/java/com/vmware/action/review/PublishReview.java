package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.util.StringUtils;

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

        String trivialText = commitConfig.publishAsTrivial ? " as trivial" : "";
        if (StringUtils.isNotBlank(commitConfig.reviewChangeDescription)) {
            log.info("Publishing review request {}{} with change description \"{}\"", reviewRequest.id, trivialText, commitConfig.reviewChangeDescription);
        } else {
            log.info("Publishing review request {}{}", reviewRequest.id, trivialText);
        }

        reviewBoard.publishReview(reviewRequest.getDraftLink(), commitConfig.reviewChangeDescription, commitConfig.publishAsTrivial);
        reviewRequest.isPublic = true;
        log.info("Successfully published review request");
    }
}
