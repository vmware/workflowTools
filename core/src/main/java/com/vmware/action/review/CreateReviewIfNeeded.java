package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Only creates a new review if there is not an existing review url in the commit.")
public class CreateReviewIfNeeded extends BaseCommitAction {
    private ReviewBoard reviewBoard;

    public CreateReviewIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.hasReviewNumber()) {
            return "commit already has review " + draft.id;
        }
        return super.cannotRunAction();
    }

    @Override
    public void preprocess() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void process() {
        log.debug("Creating new review");
        draft.reviewRequest = reviewBoard.createReviewRequest(config.reviewBoardRepository);
        draft.id = draft.reviewRequest.id;
        log.info("Created new review {}", draft.reviewRequest.id);
    }
}
