package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Sets the git commit details from the associated review request. Uses published review info only.")
public class SetCommitDetailsFromReview extends BaseCommitAction {
    public SetCommitDetailsFromReview(WorkflowConfig config) {
        super(config);
    }

    private ReviewBoard reviewBoard;

    @Override
    public void asyncSetup() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void preprocess() {
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(config.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        String reviewId = StringUtils.isInteger(draft.id) ? draft.id : config.reviewRequestId;
        if (!StringUtils.isInteger(reviewId)) {
            log.info("No review request id specified for retrieving commit details");
            reviewId = String.valueOf(InputUtils.readValueUntilValidInt("Review request id "));
        }


        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(Integer.parseInt(reviewId));

        ReviewRequestDraft reviewAsDraft = reviewRequest.asDraft();
        if (StringUtils.isBlank(reviewRequest.summary) && StringUtils.isBlank(reviewRequest.description)) { // populate values from draft
            reviewAsDraft = reviewBoard.getReviewRequestDraftWithExceptionHandling(reviewRequest.getDraftLink());
        }
        if (draft == null) {
            throw new RuntimeException("Summary and description and blank for review request " + reviewId + " and no draft found for request");
        }
        log.info("Using review request {} ({}) for patching", reviewAsDraft.id, reviewRequest.summary);
        draft.summary = StringUtils.truncateStringIfNeeded(reviewAsDraft.summary, config.maxSummaryLength);
        draft.description = StringUtils.addNewLinesIfNeeded(reviewAsDraft.description, config.maxDescriptionLength, 0);

        String testingDone = stripJenkinsJobBuildsFromTestingDone(reviewAsDraft.testingDone, config.jenkinsUrl);
        testingDone = stripBuildwebJobBuildsFromTestingDone(testingDone, config.buildwebUrl);
        testingDone =  StringUtils.addNewLinesIfNeeded(testingDone, config.maxDescriptionLength, "Testing Done: " .length());
        draft.testingDone = testingDone;
        if (StringUtils.isBlank(reviewAsDraft.bugNumbers)) {
            draft.bugNumbers = config.noBugNumberLabel;
        } else {
            draft.bugNumbers = reviewAsDraft.bugNumbers;
        }
        draft.reviewedBy = reviewAsDraft.reviewedBy;
    }

    private String stripBuildwebJobBuildsFromTestingDone(String testingDone, String buildwebUrl) {
        String patternToSearchFor = "\nBuild\\s+" + buildwebUrl + "\\S+";
        return testingDone.replaceAll(patternToSearchFor, "").trim();
    }

    private String stripJenkinsJobBuildsFromTestingDone(String testingDone, String jenkinsUrl) {
        String patternToSearchFor = "\nBuild\\s+" + jenkinsUrl + "/job/[\\w-]+/\\d+/*";
        return testingDone.replaceAll(patternToSearchFor, "").trim();
    }
}