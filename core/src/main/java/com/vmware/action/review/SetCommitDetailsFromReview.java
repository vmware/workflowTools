package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.reviewboard.domain.ReviewRequest;
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
        String reviewId = StringUtils.isInteger(draft.id) ? draft.id : config.reviewRequestForPatching;
        if (!StringUtils.isInteger(reviewId)) {
            log.info("No review request id specified for retreiving commit details");
            reviewId = String.valueOf(InputUtils.readValueUntilValidInt("Review request id "));
        }

        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(Integer.parseInt(reviewId));
        log.info("Using review request {} ({}) for patching", reviewRequest.id, reviewRequest.summary);
        draft.summary = StringUtils.truncateStringIfNeeded(reviewRequest.summary, config.maxSummaryLength);
        draft.description = StringUtils.addNewLinesIfNeeded(reviewRequest.description, config.maxDescriptionLength, 0);

        String testingDone = stripJenkinsJobBuildsFromTestingDone(reviewRequest.testingDone, config.jenkinsUrl);
        testingDone =  StringUtils.addNewLinesIfNeeded(testingDone, config.maxDescriptionLength, "Testing Done: " .length());
        draft.testingDone = testingDone;
        draft.bugNumbers = reviewRequest.getBugNumbers();
        if (StringUtils.isBlank(draft.bugNumbers)) {
            draft.bugNumbers = config.noBugNumberLabel;
        }
        draft.reviewedBy = reviewRequest.getTargetReviewersAsString();
    }

    private String stripJenkinsJobBuildsFromTestingDone(String testingDone, String jenkinsUrl) {
        String patternToSearchFor = jenkinsUrl + "/job/[\\w-]+/\\d+/*";
        return testingDone.replaceAll(patternToSearchFor, "").trim();
    }
}