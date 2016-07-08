package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.logging.LogLevel;

import static java.lang.String.format;

@ActionDescription("Uses rbt post to upload a diff for a git commit to reviewboard.")
public class UploadReviewDiffWithRbt extends BaseCommitWithReviewAction {

    public UploadReviewDiffWithRbt(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("rbt");
    }

    @Override
    public void process() {
        String command = format("rbt post --repository '%s' -r %s --tracking-branch=%s --parent=%s", config.reviewBoardRepository, draft.id, config.trackingBranch, config.parentBranch);
        CommandLineUtils.executeCommand(git.getRootDirectory(), command, null, LogLevel.INFO);
    }
}
