package com.vmware.action.review;

import com.vmware.action.base.AbstractCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.DiffToUpload;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Uploads a git diff for the review. The parent ref used is defined by the parentBranch config property.")
public class UploadReviewDiff extends AbstractCommitWithReviewAction {
    public UploadReviewDiff(WorkflowConfig config) throws IOException, URISyntaxException, IllegalAccessException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        DiffToUpload diffToUpload = createReviewRequestDiff();
        if (diffToUpload.hasEmptyDiff()) {
            log.warn("The diff for this review was empty!");
            return;
        }
        log.info("Uploading review diff for review {}", draft.reviewRequest.id);
        reviewBoard.createReviewRequestDiff(draft.reviewRequest.getDiffsLink(), diffToUpload);
        log.info("Successfully uploaded review diff");
    }

    private DiffToUpload createReviewRequestDiff() throws IOException, URISyntaxException {
        String reviewBoardVersion = reviewBoard.getVersion();
        boolean supportsDiffWithRenames = reviewBoardVersion.compareTo("1.7") >= 0;
        log.debug("Review board version: {}, Supports renames {}", reviewBoardVersion, supportsDiffWithRenames);

        DiffToUpload diff = new DiffToUpload();
        String mergeBase = git.mergeBase(config.mergeParent, "HEAD");
        diff.path = git.diff(config.parentBranch, "HEAD", supportsDiffWithRenames);
        diff.parent_diff_path = git.diff(mergeBase, config.parentBranch, supportsDiffWithRenames);
        return diff;
    }

}
