package com.vmware.action.base;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestStatus;
import com.vmware.util.DateUtils;
import com.vmware.util.logging.Padder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public abstract class BaseBatchCloseReviews extends BaseAction {

    protected ReviewBoard reviewBoard;
    private String reason;
    private int daysElapsedBeforeClose;

    public BaseBatchCloseReviews(WorkflowConfig config, String reason, int daysElapsedBeforeClose) {
        super(config);
        this.reason = reason;
        this.daysElapsedBeforeClose = daysElapsedBeforeClose;
    }

    @Override
    public void preprocess() {
        this.reviewBoard = serviceLocator.getReviewBoard();
    }

    public void closeReviews(ReviewRequest[] openRequests) {
        Padder titlePadder = new Padder("Closing reviews with {} that are older than {} days", reason, String.valueOf(daysElapsedBeforeClose));
        titlePadder.infoTitle();
        if (openRequests.length == 0) {
            log.info("No matching review requests for user " + config.username);
            return;
        }

        for (ReviewRequest openRequest: openRequests) {
            long workTimeElapsedSinceLastUpdateInMinutes = DateUtils.workWeekMinutesBetween(openRequest.lastUpdated, new Date());
            long workTimeElapsedSinceLastUpdateInDays = TimeUnit.DAYS.convert(workTimeElapsedSinceLastUpdateInMinutes, TimeUnit.MINUTES);
            if (workTimeElapsedSinceLastUpdateInDays >= daysElapsedBeforeClose) {
                log.info("");
                log.info("Marking review request {} (last updated on {}) as submitted", openRequest.id, openRequest.lastUpdated.toString());
                log.info("Review summary: " + openRequest.summary);
                openRequest.status = ReviewRequestStatus.submitted;
                openRequest.description = String.format("Marked review that has had %s for more than %s days as submitted", reason, daysElapsedBeforeClose);
                reviewBoard.updateReviewRequest(openRequest);
                log.info("Successfully marked review request {} as submitted", openRequest.id);
            }
        }
        titlePadder.infoTitle();
    }
}
