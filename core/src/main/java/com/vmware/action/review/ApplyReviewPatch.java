package com.vmware.action.review;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.scm.Perforce;
import com.vmware.scm.diff.PerforceDiffToGitConverter;
import com.vmware.util.IOUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.Padder;
import com.vmware.util.StringUtils;

import java.io.File;

@ActionDescription("Applies a specified review request's diff against the local repository.")
public class ApplyReviewPatch extends BaseAction {

    private ReviewBoard reviewBoard;

    public ApplyReviewPatch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        reviewBoard = serviceLocator.getReviewBoard();
    }

    @Override
    public void process() {
        if (config.reviewRequestForPatching == 0) {
            log.info("No review request id specified as source for patch");
            config.reviewRequestForPatching = InputUtils.readValueUntilValidInt("Review request id for patch");
        }

        ReviewRequest reviewRequest = reviewBoard.getReviewRequestById(config.reviewRequestForPatching);
        Repository repository = reviewBoard.getRepository(reviewRequest.getRepositoryLink());
        ReviewRequestDiff[] diffs = reviewBoard.getDiffsForReviewRequest(reviewRequest.getDiffsLink());
        log.info("Using review request {} ({})", reviewRequest.id, reviewRequest.summary);

        if (diffs.length == 0) {
            throw new IllegalArgumentException(String.format("Review request %s does not have any diffs",
                    config.reviewRequestForPatching));
        }

        int diffSelection = diffs.length - 1;
        if (!config.alwaysUseLatestDiff && diffs.length > 1) {
            diffSelection = InputUtils.readSelection(diffs, "Select diff to apply");
        }

        String diffData = reviewBoard.getDiffData(diffs[diffSelection].getSelfLink());

        if (repository.tool.toLowerCase().contains("perforce")) {
            Perforce perforce = serviceLocator.getPerforce();
            diffData = new PerforceDiffToGitConverter(perforce).convert(diffData);
        }

        log.debug("Patch to apply\n{}", diffData);

        File currentDir = new File(System.getProperty("user.dir"));
        IOUtils.write(new File(currentDir.getPath() + "/workflow.patch"), diffData);

        log.info("Checking if diff {} applies", diffSelection + 1);
        String checkResult = git.applyDiff(diffData, false);
        if (StringUtils.isNotBlank(checkResult)) {
            log.error("Checking of diff failed!\n{}", checkResult);
            return;
        }

        log.info("Applying diff {}", diffSelection + 1);
        String result = git.applyDiff(diffData, false);

        if (StringUtils.isBlank(result.trim())) {
            log.info("Diff successfully applied");
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
