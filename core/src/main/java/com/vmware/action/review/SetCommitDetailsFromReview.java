package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.util.StringUtils;

@ActionDescription("Sets the git commit details from the associated review request. Uses published review info only.")
public class SetCommitDetailsFromReview extends BaseCommitUsingReviewBoardAction {
    public SetCommitDetailsFromReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        ReviewRequest associatedReview = draft.reviewRequest;
        draft.summary = StringUtils.truncateStringIfNeeded(associatedReview.summary, config.maxSummaryLength);
        draft.description = StringUtils.addNewLinesIfNeeded(associatedReview.description, config.maxDescriptionLength, 0);

        String testingDone = stripJenkinsJobBuildsFromTestingDone(associatedReview.testingDone, config.jenkinsUrl);
        testingDone =  StringUtils.addNewLinesIfNeeded(testingDone, config.maxDescriptionLength, "Testing Done: " .length());
        draft.testingDone = testingDone;
        draft.bugNumbers = associatedReview.getBugNumbers();
        if (StringUtils.isBlank(draft.bugNumbers)) {
            draft.bugNumbers = config.noBugNumberLabel;
        }
        draft.reviewedBy = associatedReview.getTargetReviewersAsString();
    }

    private String stripJenkinsJobBuildsFromTestingDone(String testingDone, String jenkinsUrl) {
        String patternToSearchFor = jenkinsUrl + "/job/[\\w-]+/\\d+/*";
        return testingDone.replaceAll(patternToSearchFor, "").trim();
    }
}