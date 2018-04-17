package com.vmware.action.batch;

import com.vmware.action.base.BaseBatchCloseReviews;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Checks review board for assigned reviews that have a comment about being submitted." +
        "\nReviews older in days than the config property value closeOldSubmittedReviewsAfter are marked as submitted.")
public class HardSubmitOldSoftSubmittedReviews extends BaseBatchCloseReviews {

    public HardSubmitOldSoftSubmittedReviews(WorkflowConfig config) {
        super(config, "submitted comment", config.reviewBoardConfig.closeOldSubmittedReviewsAfter);
    }

    @Override
    public void process() {
        super.closeReviews(reviewBoard.getOpenReviewRequestsWithSubmittedComment());
    }
}
