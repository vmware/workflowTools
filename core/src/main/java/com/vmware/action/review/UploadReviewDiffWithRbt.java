package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;

import java.util.logging.Level;

@ActionDescription("Uses rbt post to upload a diff to reviewboard, use for git p4 or perforce for example.")
public class UploadReviewDiffWithRbt extends BaseCommitWithReviewAction {

    public UploadReviewDiffWithRbt(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!CommandLineUtils.isCommandAvailable("rbt")) {
            return "rbt is not installed";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        CommandLineUtils.executeCommand(git.getRootDirectory(), "rbt post -r " + draft.id, null, Level.INFO);
    }
}
