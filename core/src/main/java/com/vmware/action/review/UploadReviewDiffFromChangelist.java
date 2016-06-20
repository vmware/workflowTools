package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;

import java.io.File;
import java.util.logging.Level;

@ActionDescription("Uses rbt post to upload a changelist as a diff to reviewboard, only for perforce")
public class UploadReviewDiffFromChangelist extends BaseCommitWithReviewAction {

    public UploadReviewDiffFromChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!CommandLineUtils.isCommandAvailable("rbt")) {
            return "rbt is not installed";
        }
        if (!CommandLineUtils.isCommandAvailable("p4")) {
            return "p4 is not installed";
        }
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no matching changelist found, run createPendingChangelist as part of workflow";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        File clientDirectory = perforce.getClientDirectory();
        if (clientDirectory == null) {
            throw new IllegalArgumentException("Client directory not found, is git-p4.client set?");
        }
        String output = CommandLineUtils.executeCommand(clientDirectory,
                "rbt post -r " + draft.id + " " + draft.perforceChangelistId, null, Level.INFO);
        if (!output.contains("Review request #" + draft.id + " posted")) {
            log.error("Failed to upload diff successfully\n{}", output);
        }
    }
}
