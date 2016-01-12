package com.vmware.action.review;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.utils.IOUtils;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

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

        File currentDir = new File(System.getProperty("user.dir"));
        IOUtils.write(new File(currentDir.getPath() + "/workflow.patch"), diffData);

        log.info("Applying diff {}", diffSelection + 1);
        String result = git.applyDiff(diffData);

        if (StringUtils.isBlank(result.trim())) {
            log.info("Diff successfully applied");
        } else {
            printDiffResult(result);
        }
    }

    private void printDiffResult(String result) {
        log.info("Potential issues with applying diff");
        Padder padder = new Padder("Patch Output");
        padder.infoTitle();
        log.info(result);
        padder.infoTitle();
    }
}
