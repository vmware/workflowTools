package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseSetShipItReviewersList extends BaseCommitWithReviewAction {


    public BaseSetShipItReviewersList(WorkflowConfig config) {
        super(config);
    }

    protected void checkShipItsForReview(ReviewRequestDraft draft) {
        // reuse the result from other actions like ExitIfReviewHasNoShipIts or CheckStatusOfReviewShipIts
        if (StringUtils.isNotBlank(draft.shipItReviewers)) {
            return;
        }

        int reviewNumber = draft.reviewRequest.id;
        log.info("Checking ship its for review {}", reviewNumber);

        String updatedReviewers = reviewBoard.getShipItReviewerList(draft.reviewRequest);

        draft.shipItReviewers = updatedReviewers;
        if (updatedReviewers.isEmpty()) {
            log.info("No ship its yet");
            return;
        }
        log.info("Ship its from users ({})", updatedReviewers);
    }

}
