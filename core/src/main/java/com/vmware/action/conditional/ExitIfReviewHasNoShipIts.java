package com.vmware.action.conditional;

import com.vmware.action.base.BaseSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Exits if a non trivial review does not have review ship its or a review URL.")
public class ExitIfReviewHasNoShipIts extends BaseSetShipItReviewersList {

    public ExitIfReviewHasNoShipIts(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        checkShipItsForReview(draft);

        if (StringUtils.isNotBlank(draft.shipItReviewers)) {
            return;
        }

        log.info("");
        log.info("Exiting as the review has no ship its.");
        System.exit(0);
    }

}
