package com.vmware.action.batch;

import com.vmware.action.base.BaseBatchCloseReviews;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Checks review board for assigned reviews that have at least one ship it." +
        "\nReviews older in days than the config property value closeOldShipItReviewsAfter are marked as submitted.")
public class HardSubmitOldShipItReviews extends BaseBatchCloseReviews {

    public HardSubmitOldShipItReviews(WorkflowConfig config) {
        super(config, "ship its", config.reviewBoardConfig.closeOldShipItReviewsAfter);
    }

    @Override
    public void process() {
        super.closeReviews(reviewBoard.getOpenReviewRequestsWithShipIts().review_requests);
    }
}
