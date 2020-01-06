package com.vmware.action.review;

import com.vmware.action.base.BaseSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Sets the reviewer list for the commit as the list of reviewers who have given the associated review a ship it.")
public class SetReviewedByAsShipItsList extends BaseSetShipItReviewersList {
    public SetReviewedByAsShipItsList(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        // reuse the result from other actions like ExitIfReviewHasNoShipIts or CheckStatusOfReviewShipIts
        if (StringUtils.isNotEmpty(draft.shipItReviewers)) {
            draft.reviewedBy = draft.shipItReviewers;
            draft.shipItReviewers = null;
            return;
        }

        checkShipItsForReview(draft);

        if (StringUtils.isNotEmpty(draft.shipItReviewers)) {
            draft.reviewedBy = draft.shipItReviewers;
        }
    }
}
