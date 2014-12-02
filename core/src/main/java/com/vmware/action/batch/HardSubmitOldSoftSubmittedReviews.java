package com.vmware.action.batch;

import com.vmware.action.base.AbstractBatchCloseReviews;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Checks review board for assigned reviews that have a comment about being submitted." +
        "\nReviews older in days than the config property value closeOldSubmittedReviewsAfter are marked as submitted.")
public class HardSubmitOldSoftSubmittedReviews extends AbstractBatchCloseReviews {

    public HardSubmitOldSoftSubmittedReviews(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config, "submitted comment", config.closeOldSubmittedReviewsAfter);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        super.closeReviews(reviewBoard.getOpenReviewRequestsWithSubmittedComment());
    }
}
