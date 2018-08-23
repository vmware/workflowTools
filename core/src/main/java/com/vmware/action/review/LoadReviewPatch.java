package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import static com.vmware.util.input.InputUtils.readValueUntilValidInt;

@ActionDescription("Loads diff data for the specified review.")
public class LoadReviewPatch extends BaseCommitAction {

    private ReviewBoard reviewBoard;

    public LoadReviewPatch(WorkflowConfig config) {
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
        if (reviewBoardConfig.reviewRequestId == null) {
            log.info("No review request id specified as source for patch");
            reviewBoardConfig.reviewRequestId = String.valueOf(readValueUntilValidInt("Review request id for patch"));
        }
        int reviewId = Integer.parseInt(reviewBoardConfig.reviewRequestId);
        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(reviewId);
        Repository repository = reviewBoard.getRepository(reviewRequest.getRepositoryLink());
        ReviewRequestDiff[] diffs = reviewBoard.getDiffsForReviewRequest(reviewRequest.getDiffsLink());
        log.info("Using review request {} ({}) for patching", reviewRequest.id, reviewRequest.summary);

        if (diffs.length == 0) {
            throw new FatalException("Review request {} does not have any diffs", reviewBoardConfig.reviewRequestId);
        }

        int diffSelection = diffs.length - 1;
        if (!patchConfig.alwaysUseLatestDiff && diffs.length > 1) {
            diffSelection = InputUtils.readSelection(diffs, "Select diff to apply");
        }

        draft.repoType = repository.getRepoType();
        draft.draftPatchData = reviewBoard.getDiffData(diffs[diffSelection].getSelfLink());
    }
}
