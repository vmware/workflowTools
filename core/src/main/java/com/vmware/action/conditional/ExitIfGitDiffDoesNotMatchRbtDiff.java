package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.DiffUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Compares the git diff for a review with the diff rbt produces.")
public class ExitIfGitDiffDoesNotMatchRbtDiff extends BaseCommitUsingReviewBoardAction {

    public ExitIfGitDiffDoesNotMatchRbtDiff(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("rbt");
    }

    @Override
    public void process() {
        String rbtDiff = CommandLineUtils.executeCommand("rbt diff --server " + commitConfig.reviewboardUrl, LogLevel.DEBUG);
        String parentRef = gitRepoConfig.parentBranchPath();
        log.info("Using parent ref {} for git diff", parentRef);
        String gitDiff = git.diff(parentRef, "HEAD", reviewBoard.supportsDiffWithRenames());

        if (rbtDiff.equals(gitDiff)) {
            log.info("Diffs match exactly");
        } else {
            inspectDiffContents(rbtDiff, gitDiff);
        }
    }

    private void inspectDiffContents(String rbtDiff, String gitDiff) {
        String gitDiffWithExtraNewline = gitDiff + "\n";
        if (gitDiffWithExtraNewline.equals(rbtDiff)) {
            log.info("Diffs match if git diff has extra newline");
        } else {
            String reasonForMotMatching = DiffUtils.compareDiffContent(gitDiff, rbtDiff, "git", "rbt");
            if (StringUtils.isNotBlank(reasonForMotMatching)) {
                log.error("Git diff didn't match rbt diff\n{}\n", reasonForMotMatching);
                cancelWithErrorMessage("diffs did not match");
            } else {
                log.info("Diffs are the same");
            }
        }
    }
}
