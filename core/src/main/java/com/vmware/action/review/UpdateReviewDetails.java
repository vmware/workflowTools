package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

@ActionDescription("Updates the review request draft details (summary, description, testing done, bug number, groups, people).")
public class UpdateReviewDetails extends BaseCommitUsingReviewBoardAction {
    public UpdateReviewDetails(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        ReviewRequest reviewRequest = draft.reviewRequest;
        log.info("Updating information for review " + reviewRequest.id);

        ReviewRequestDraft existingDraft = reviewBoard.getReviewRequestDraftWithExceptionHandling(reviewRequest.getDraftLink());
        if (existingDraft != null) {
            draft.targetGroups = existingDraft.targetGroups;
        }

        if (reviewBoardConfig.disableMarkdown) {
            log.info("Sending description and testing done as plain text");
            draft.descriptionTextType = "plain";
            draft.testingDoneTextType = "plain";
        } else {
            log.info("Sending description and testing done as markdown text");
            draft.descriptionTextType = "markdown";
            draft.testingDoneTextType = "markdown";
        }

        draft.updateTargetGroupsIfNeeded(reviewBoardConfig.targetGroups);
        draft.addExtraTargetGroupsIfNeeded();
        reviewBoard.updateReviewRequestDraft(reviewRequest.getDraftLink(), draft);
        log.info("Successfully updated review information");
    }
}