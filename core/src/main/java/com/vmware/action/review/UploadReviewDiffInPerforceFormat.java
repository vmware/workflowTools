package com.vmware.action.review;

import com.vmware.scm.Perforce;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.DiffToUpload;
import com.vmware.scm.diff.GitDiffToPerforceConverter;

import static java.lang.String.format;

@ActionDescription("Uploads a git diff in perforce format for the review. The parent ref used is defined by the parentBranch config property.")
public class UploadReviewDiffInPerforceFormat extends UploadReviewDiff {

    private Perforce perforce;

    public UploadReviewDiffInPerforceFormat(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        super.preprocess();
        this.perforce = serviceLocator.getPerforce();
    }

    protected DiffToUpload createReviewRequestDiff() {
        log.info("Converting git diff into a diff in perforce format against parent branch {}", config.parentBranch);
        String reviewBoardVersion = reviewBoard.getVersion();
        boolean supportsDiffWithRenames = reviewBoardVersion.compareTo("1.7") >= 0;
        log.debug("Review board version: {}, Supports renames {}", reviewBoardVersion, supportsDiffWithRenames);

        DiffToUpload diff = new DiffToUpload();
        String mergeBase = git.mergeBase(config.trackingBranch, "HEAD");
        GitDiffToPerforceConverter diffConverter = new GitDiffToPerforceConverter(perforce, git.lastSubmittedChangelistInfo()[1]);
        diff.path = diffConverter.convert(git.diff(config.parentBranch, "HEAD", supportsDiffWithRenames));
        diff.parent_diff_path = diffConverter.convert(git.diff(mergeBase, config.parentBranch, supportsDiffWithRenames));
        return diff;
    }
}
