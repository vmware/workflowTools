package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.utils.input.InputUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Read a review id from the user. Set that review id as the review id for the commit.")
public class SetReviewId extends BaseCommitAction {

    private ReviewBoard reviewBoard;

    public SetReviewId(WorkflowConfig config) throws IllegalAccessException, IOException,
            URISyntaxException {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        log.info("Please enter review id for the commit");
        int reviewId = InputUtils.readValueUntilValidInt("Review ID");
        draft.id = reviewId;
        draft.reviewRequest = reviewBoard.getReviewRequestById(reviewId);
        log.info("Using review {} ({})", reviewId, draft.reviewRequest.summary);
    }
}
