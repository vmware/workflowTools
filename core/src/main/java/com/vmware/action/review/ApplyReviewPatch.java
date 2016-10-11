package com.vmware.action.review;

import com.vmware.action.BaseAction;
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
import java.util.ArrayList;
import java.util.List;

@ActionDescription("Applies a specified review request's diff against the local repository.")
public class ApplyReviewPatch extends BaseCommitAction {

    private ReviewBoard reviewBoard;

    public ApplyReviewPatch(WorkflowConfig config) {
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
        if (config.reviewRequestForPatching == null) {
            log.info("No review request id specified as source for patch");
            config.reviewRequestForPatching = String.valueOf(InputUtils.readValueUntilValidInt("Review request id for patch"));
        }
        int reviewId = Integer.parseInt(config.reviewRequestForPatching);
        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(reviewId);
        Repository repository = reviewBoard.getRepository(reviewRequest.getRepositoryLink());
        ReviewRequestDiff[] diffs = reviewBoard.getDiffsForReviewRequest(reviewRequest.getDiffsLink());
        log.info("Using review request {} ({}) for patching", reviewRequest.id, reviewRequest.summary);

        if (diffs.length == 0) {
            throw new IllegalArgumentException(String.format("Review request %s does not have any diffs",
                    config.reviewRequestForPatching));
        }

        int diffSelection = diffs.length - 1;
        if (!config.alwaysUseLatestDiff && diffs.length > 1) {
            diffSelection = InputUtils.readSelection(diffs, "Select diff to apply");
        }

        String diffData = reviewBoard.getDiffData(diffs[diffSelection].getSelfLink());

        List<FileChange> fileChanges = null;
        boolean isPerforceClient = !git.workingDirectoryIsInGitRepo();
        if (repository.tool.toLowerCase().contains("perforce")) {
            PerforceDiffToGitConverter diffConverter = new PerforceDiffToGitConverter();
            diffData = diffConverter.convert(diffData);
            fileChanges = diffConverter.getFileChanges();
        } else if (isPerforceClient){
            Perforce perforce = serviceLocator.getPerforce();
            GitDiffToPerforceConverter diffConverter = new GitDiffToPerforceConverter(perforce, "");
            diffConverter.convert(diffData);
            fileChanges = diffConverter.getFileChanges();
        }

        if (isPerforceClient) {
            if (StringUtils.isBlank(draft.perforceChangelistId)) {
                throw new RuntimeException("No changelist specified to apply patch to");
            }
            Perforce perforce = serviceLocator.getPerforce();
            perforce.syncPerforceFiles(fileChanges, "");
            perforce.openFilesForEditIfNeeded(draft.perforceChangelistId, fileChanges);
        }
        log.trace("Patch to apply\n{}", diffData);

        File currentDir = new File(System.getProperty("user.dir"));
        IOUtils.write(new File(currentDir.getPath() + "/workflow.patch"), diffData);

        log.info("Checking if diff {} applies", diffSelection + 1);
        String checkResult = git.applyDiff(diffData, true);
        if (StringUtils.isNotBlank(checkResult)) {
            log.error("Checking of diff failed!\n{}", checkResult);
            return;
        }

        log.info("Applying diff {}", diffSelection + 1);
        String result = git.applyDiff(diffData, false);

        if (StringUtils.isBlank(result.trim())) {
            log.info("Diff successfully applied");
            if (isPerforceClient) {
                serviceLocator.getPerforce().renameAddOrDeleteFiles(draft.perforceChangelistId, fileChanges);
            }
        } else {
            printDiffResult(result);
        }
    }

    private void printDiffResult(String result) {
        log.warn("Potential issues with applying diff");
        Padder padder = new Padder("Patch Output");
        padder.warnTitle();
        log.info(result);
        padder.warnTitle();
    }
}
