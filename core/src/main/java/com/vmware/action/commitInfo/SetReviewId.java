package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.exception.NotFoundException;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Read a review id from the user. Set that review id as the review id for the commit.")
public class SetReviewId extends BaseCommitAction {

    private ReviewBoard reviewBoard;

    public SetReviewId(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        if (StringUtils.isInteger(draft.id)) {
            log.info("Existing review id {}", draft.id);
        }
        log.info("Please enter review id for the commit");
        int reviewId = InputUtils.readValueUntilValidInt("Review ID");
        draft.id = String.valueOf(reviewId);
        draft.reviewRequest = reviewBoard.getReviewRequestById(reviewId);
        String summary = draft.reviewRequest.summary;
        if (StringUtils.isEmpty(summary)) {
            try {
                summary = reviewBoard.getReviewRequestDraft(draft.reviewRequest.getDraftLink()).summary;
            } catch (NotFoundException nfe) {
                log.warn("Summary was blank, and no draft was present");
            }
        }
        if (StringUtils.isNotEmpty(summary)) {
            summary = " - " + summary;
        }
        log.info("Using review {}{}", reviewId, summary);
    }
}
