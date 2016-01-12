package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Always creates a new review, even if there is an existing review url in the commit.")
public class CreateReview extends BaseCommitAction {
    private ReviewBoard reviewBoard;

    public CreateReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void process() {
        if (!draft.hasReviewNumber()) {
            log.info("Creating new review");
        } else {
            log.info("Creating new review to replace review {} for this commit", draft.id);
        }

        draft.reviewRequest = reviewBoard.createReviewRequest(config.reviewBoardRepository);
        draft.id = draft.reviewRequest.id;
        log.info("Created empty review {}", draft.id);
    }
}
