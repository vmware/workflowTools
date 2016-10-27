package com.vmware.action.batch;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.reviewboard.domain.ReviewRequestStatus;
import com.vmware.reviewboard.domain.ReviewRequests;
import com.vmware.util.input.InputUtils;

@ActionDescription("Shows the user a list of their unpublished reviews, user can select which ones to discard.")
public class DiscardUnpublishedReviews extends BaseAction {

    private ReviewBoard reviewBoard;

    public DiscardUnpublishedReviews(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void process() {
        ReviewRequests reviewRequests = reviewBoard.getReviewRequests(ReviewRequestStatus.pending);
        for (ReviewRequest reviewRequest : reviewRequests.review_requests) {
            if (reviewRequest.isPublic) {
                continue;
            }
            ReviewRequestDraft draft = reviewBoard.getReviewRequestDraftWithExceptionHandling(reviewRequest.getDraftLink());
            if (draft == null) {
                continue;
            }
            String label = "Discard review " + reviewRequest.id + " (" + draft.summary + ")[Y/N]";
            String confirm = InputUtils.readValueUntilNotBlank(label);
            if ("y".equalsIgnoreCase(confirm)) {
                reviewRequest.status = ReviewRequestStatus.discarded;
                reviewBoard.updateReviewRequest(reviewRequest);
                log.info("Successfully discarded review request {}", reviewRequest.id);
            }
        }
    }
}
