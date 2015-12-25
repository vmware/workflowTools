package com.vmware.action.conditional;

import com.vmware.action.base.AbstractSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Exits if a non trivial review does not have review ship its or a review URL.")
public class ExitIfReviewHasNoShipIts extends AbstractSetShipItReviewersList {

    public ExitIfReviewHasNoShipIts(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        checkShipItsForReview(draft);

        if (StringUtils.isNotBlank(draft.shipItReviewers)) {
            return;
        }

        log.info("");
        log.info("Exiting as the review has no ship its.");
        System.exit(0);
    }

}
