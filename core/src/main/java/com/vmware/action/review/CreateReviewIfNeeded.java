package com.vmware.action.review;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Only creates a new review if there is not an existing review url in the commit.")
public class CreateReviewIfNeeded extends AbstractCommitAction {
    private ReviewBoard reviewBoard;

    public CreateReviewIfNeeded(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        if (draft.hasReviewNumber()) {
            return;
        }

        log.debug("Creating new review");
        draft.reviewRequest = reviewBoard.createReviewRequest(config.reviewBoardRepository);
        draft.id = draft.reviewRequest.id;
        log.info("Created new review {}", draft.reviewRequest.id);
    }
}
