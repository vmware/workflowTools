package com.vmware.action.review;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.scm.FileChange;
import com.vmware.scm.Perforce;
import com.vmware.scm.diff.GitDiffToPerforceConverter;
import com.vmware.scm.diff.PerforceDiffToGitConverter;
import com.vmware.util.IOUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.Padder;
import com.vmware.util.StringUtils;

import java.io.File;
import java.util.List;

@ActionDescription("Loads diff data for the specified review")
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
        reviewBoard.setupAuthenticatedConnectionWithLocalTimezone(config.reviewBoardDateFormat);
    }

    @Override
    public void process() {
        if (config.reviewRequestId == null) {
            log.info("No review request id specified as source for patch");
            config.reviewRequestId = String.valueOf(InputUtils.readValueUntilValidInt("Review request id for patch"));
        }
        int reviewId = Integer.parseInt(config.reviewRequestId);
        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(reviewId);
        Repository repository = reviewBoard.getRepository(reviewRequest.getRepositoryLink());
        ReviewRequestDiff[] diffs = reviewBoard.getDiffsForReviewRequest(reviewRequest.getDiffsLink());
        log.info("Using review request {} ({}) for patching", reviewRequest.id, reviewRequest.summary);

        if (diffs.length == 0) {
            throw new IllegalArgumentException(String.format("Review request %s does not have any diffs",
                    config.reviewRequestId));
        }

        int diffSelection = diffs.length - 1;
        if (!config.alwaysUseLatestDiff && diffs.length > 1) {
            diffSelection = InputUtils.readSelection(diffs, "Select diff to apply");
        }

        draft.repoType = repository.tool;
        draft.draftDiffData = reviewBoard.getDiffData(diffs[diffSelection].getSelfLink());
    }
}
