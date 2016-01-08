package com.vmware.action.review;

import com.vmware.action.base.BaseSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Checks if the review has ship its.")
public class CheckStatusOfReviewShipIts extends BaseSetShipItReviewersList {
    public CheckStatusOfReviewShipIts(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void process() throws IOException, URISyntaxException {
        checkShipItsForReview(draft);
    }


}
