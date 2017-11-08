package com.vmware.action.review;

import com.vmware.action.base.BaseSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Checks if the review has ship its.")
public class CheckStatusOfReviewShipIts extends BaseSetShipItReviewersList {
    public CheckStatusOfReviewShipIts(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        checkShipItsForReview(draft);
    }


}
