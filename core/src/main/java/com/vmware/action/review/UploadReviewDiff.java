package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.DiffToUpload;
import com.vmware.reviewboard.domain.RepoType;
import com.vmware.util.scm.diff.GitDiffToPerforceConverter;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;

import static java.lang.String.format;

@ActionDescription("Uploads a git diff for the review. The parent ref used is defined by the parentBranch config property.")
public class UploadReviewDiff extends BaseCommitUsingReviewBoardAction {
    public UploadReviewDiff(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        RepoType repoType = draft.repoType;
        if (repoType == RepoType.perforce) {
            if (!git.workingDirectoryIsInGitRepo()) { // in non git repo, run rbt
                File clientDirectory = serviceLocator.getPerforce().getWorkingDirectory();
                uploadDiffUsingRbt(clientDirectory, draft.perforceChangelistId);
            } else {
                // assuming need to create perforce compatible diff from git
                checkThatPerforceConfigIsValid();
                uploadReviewDiff();
            }
        } else if (repoType == RepoType.git) {
            uploadReviewDiff();
        } else {
            log.info("No special support for repo type {}, just doing rbt post", repoType);
            File workingDirectory = new File(System.getProperty("user.dir"));
            uploadDiffUsingRbt(workingDirectory, null);
        }
    }

    protected void uploadReviewDiff() {
        RepoType repoType = draft.repoType;
        DiffToUpload diffToUpload;
        if (repoType == RepoType.perforce) {
            diffToUpload = createPerforceReviewRequestDiffFromGit();
        } else {
            diffToUpload = createReviewRequestDiff();
        }

        if (diffToUpload.hasEmptyDiff()) {
            log.warn("The diff for this review was empty!");
            return;
        }

        log.info("Uploading review diff for review {}", draft.reviewRequest.id);
        reviewBoard.createReviewRequestDiff(draft.reviewRequest.getDiffsLink(), diffToUpload);
        log.info("Successfully uploaded review diff");
    }

    protected void uploadDiffUsingRbt(File workingDirectory, String changelistId) {
        String changelistIdText = changelistId != null ? " " + changelistId : "";
        String command = format("rbt post -r %s%s", draft.id, changelistIdText);
        runRbtCommand(workingDirectory, command);
    }

    private void runRbtCommand(File workingDirectory, String command) {
        String output = CommandLineUtils.executeCommand(workingDirectory, command, null, LogLevel.INFO);
        if (!output.contains("Review request #" + draft.id + " posted")) {
            throw new RuntimeException("Failed to upload diff successfully\n" + output);
        }
    }

    private DiffToUpload createReviewRequestDiff() {
        String trackingBranchPath = gitRepoConfig.trackingBranchPath();
        String parentPath = gitRepoConfig.parentBranchPath();
        log.info("Creating git diff using tracking branch {} against parent {}", trackingBranchPath, parentPath);
        String reviewBoardVersion = reviewBoard.getVersion();
        boolean supportsDiffWithRenames = reviewBoardVersion.compareTo("1.7") >= 0;
        log.debug("Review board version: {}, Supports renames {}", reviewBoardVersion, supportsDiffWithRenames);

        DiffToUpload diff = new DiffToUpload();
        String mergeBase = git.mergeBase(trackingBranchPath, "HEAD");
        diff.path = git.diffAsByteArray(parentPath, "HEAD", supportsDiffWithRenames);
        diff.parent_diff_path = git.diffAsByteArray(mergeBase, parentPath, supportsDiffWithRenames);
        return diff;
    }

    private void checkThatPerforceConfigIsValid() {
        if (StringUtils.isEmpty(perforceClientConfig.perforceClientName)) {
            throw new RuntimeException("config value perforceClientName not set, if using git, can be set by running git config git-p4.client clientName");
        }
        // if root directory is null, then assuming it should be a perforce client
        if (!git.workingDirectoryIsInGitRepo() && StringUtils.isEmpty(draft.perforceChangelistId)) {
            throw new RuntimeException("no matching changelist found, run createPendingChangelist as part of workflow");
        }
    }

    private DiffToUpload createPerforceReviewRequestDiffFromGit() {
        log.info("Converting git diff into a diff in perforce format against parent branch {}", gitRepoConfig.parentBranchPath());
        String reviewBoardVersion = reviewBoard.getVersion();
        boolean supportsDiffWithRenames = reviewBoardVersion.compareTo("1.7") >= 0;
        log.debug("Review board version: {}, Supports renames {}", reviewBoardVersion, supportsDiffWithRenames);

        DiffToUpload diff = new DiffToUpload();
        String mergeBase = git.mergeBase(gitRepoConfig.trackingBranchPath(), "HEAD");
        GitDiffToPerforceConverter diffConverter = new GitDiffToPerforceConverter(getLoggedInPerforceClient(),
                git.lastSubmittedChangelistInfo().getChangelistId());
        diff.path = diffConverter.convertAsBytes(git.diff(gitRepoConfig.parentBranchPath(), "HEAD", supportsDiffWithRenames));
        diff.parent_diff_path = diffConverter.convertAsBytes(git.diff(mergeBase, gitRepoConfig.parentBranchPath(), supportsDiffWithRenames));
        return diff;
    }

}
