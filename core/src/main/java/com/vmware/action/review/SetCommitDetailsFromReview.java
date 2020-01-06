package com.vmware.action.review;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

import java.util.List;
import java.util.Optional;

@ActionDescription("Sets the git commit details from the associated review request. Uses published review info only.")
public class SetCommitDetailsFromReview extends BaseCommitAction {
    private ReviewBoard reviewBoard;

    public SetCommitDetailsFromReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        String reviewId = StringUtils.isInteger(draft.id) ? draft.id : reviewBoardConfig.reviewRequestId;
        if (!StringUtils.isInteger(reviewId)) {
            log.info("No review request id specified for retrieving commit details");
            reviewId = String.valueOf(InputUtils.readValueUntilValidInt("Review request id "));
        }


        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(Integer.parseInt(reviewId));

        ReviewRequestDraft reviewAsDraft = reviewRequest.asDraft();
        if (StringUtils.isEmpty(reviewAsDraft.summary) && StringUtils.isEmpty(reviewAsDraft.description)) { // populate values from draft
            reviewAsDraft = reviewBoard.getReviewRequestDraftWithExceptionHandling(reviewRequest.getDraftLink());
        }
        if (draft == null) {
            throw new RuntimeException("Summary and description and blank for review request " + reviewId + " and no draft found for request");
        }
        log.info("Using review request {} ({}) for patching", reviewAsDraft.id, reviewRequest.summary);
        draft.summary = StringUtils.truncateStringIfNeeded(reviewAsDraft.summary, commitConfig.maxSummaryLength);
        draft.description = StringUtils.addNewLinesIfNeeded(reviewAsDraft.description, commitConfig.maxDescriptionLength, 0);

        String fullReviewText = reviewRequest.summary + "\n" + reviewAsDraft.description
                + "\n" + commitConfig.getTestingDoneLabel() + reviewAsDraft.testingDone;
        ReviewRequestDraft draftConstructedFromReviewDetails = new ReviewRequestDraft(fullReviewText, commitConfig);

        String testingDone = draftConstructedFromReviewDetails.testingDone;
        testingDone =  StringUtils.addNewLinesIfNeeded(testingDone, commitConfig.maxDescriptionLength, "Testing Done: " .length());
        draft.testingDone = testingDone;
        syncJobBuilds(draftConstructedFromReviewDetails);
        if (StringUtils.isEmpty(reviewAsDraft.bugNumbers)) {
            draft.bugNumbers = commitConfig.noBugNumberLabel;
        } else {
            draft.bugNumbers = reviewAsDraft.bugNumbers;
        }
        draft.reviewedBy = reviewAsDraft.reviewedBy;
    }

    private void syncJobBuilds(ReviewRequestDraft draftConstructedFromReviewDetails) {
        List<JobBuild> existingBuilds = draft.jobBuilds;
        List<JobBuild> buildsFromReview = draftConstructedFromReviewDetails.jobBuilds;
        for (JobBuild jobBuildFromReview : buildsFromReview) {
            Optional<JobBuild> existingBuild =
                    existingBuilds.stream().filter(build -> build.url.equals(jobBuildFromReview.url)).findFirst();
            if (!existingBuild.isPresent()) {
                existingBuilds.add(jobBuildFromReview);
            } else if (jobBuildFromReview.result != null) {
                existingBuild.get().result = jobBuildFromReview.result;
            }
        }
        existingBuilds.removeIf(build -> buildsFromReview.stream()
                .noneMatch(buildFromReview -> StringUtils.equals(build.url, buildFromReview.url)));
    }
}