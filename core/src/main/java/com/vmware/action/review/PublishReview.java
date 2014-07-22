package com.vmware.action.review;

import com.vmware.action.base.AbstractCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Publishes a review on the review board server")
public class PublishReview extends AbstractCommitWithReviewAction {

    public PublishReview(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        ReviewRequest reviewRequest = draft.reviewRequest;

        try {
            reviewBoard.getReviewRequestDraft(reviewRequest.getDraftLink());
        } catch (NotFoundException e) {
            log.error("No changes detected for review request {}\n{}", draft.id, config.reviewboardUrl + "/r/" + reviewRequest.id);
            return;
        }

        log.info("Publishing review request {}", reviewRequest.id);
        reviewBoard.publishReview(reviewRequest.getDraftLink());
        reviewRequest.isPublic = true;
        log.info("Successfully published review request");
    }
}
