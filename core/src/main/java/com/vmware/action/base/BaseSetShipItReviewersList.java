package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.reviewboard.domain.UserReview;
import com.vmware.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseSetShipItReviewersList extends BaseCommitUsingReviewBoardAction {


    public BaseSetShipItReviewersList(WorkflowConfig config) {
        super(config);
    }

    protected void checkShipItsForReview(ReviewRequestDraft draft) {
        // reuse the result from other actions like ExitIfReviewHasNoShipIts or CheckStatusOfReviewShipIts
        if (StringUtils.isNotEmpty(draft.shipItReviewers)) {
            return;
        }

        int reviewNumber = draft.reviewRequest.id;
        log.info("Checking ship its for review {}", reviewNumber);

        if (!draft.reviewRequest.isPublic) {
            log.info("Review has not been published yet");
            return;
        }

        UserReview[] reviews = reviewBoard.getReviewsForReviewRequest(draft.reviewRequest.getReviewsLink());
        String updatedReviewers = getShipItReviewerList(reviews);

        draft.shipItReviewers = updatedReviewers;
        if (updatedReviewers.isEmpty()) {
            log.info("No ship its yet");
        } else {
            log.info("Ship its from users ({})", updatedReviewers);
        }
    }

    public String getShipItReviewerList(UserReview[] reviews) {
        Set<String> reviewers = new HashSet<>();
        for (UserReview review : reviews) {
            if (review.isPublic && review.ship_it) {
                reviewers.add(review.getReviewUsername());
            }
        }
        return String.join(",", reviewers);
    }
}
