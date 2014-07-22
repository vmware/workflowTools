package com.vmware.action.review;

import com.vmware.action.base.AbstractSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Sets the reviewer list for the commit as the list of reviewers who have given the associated review a ship it.")
public class SetReviewedByAsShipItsList extends AbstractSetShipItReviewersList {
    public SetReviewedByAsShipItsList(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        // reuse the result from other actions like ExitIfReviewHasNoShipIts or CheckStatusOfReviewShipIts
        if (StringUtils.isNotBlank(draft.shipItReviewers)) {
            draft.reviewedBy = draft.shipItReviewers;
            draft.shipItReviewers = null;
            return;
        }

        checkShipItsForReview(draft);

        if (StringUtils.isNotBlank(draft.shipItReviewers)) {
            draft.reviewedBy = draft.shipItReviewers;
        }
    }
}
