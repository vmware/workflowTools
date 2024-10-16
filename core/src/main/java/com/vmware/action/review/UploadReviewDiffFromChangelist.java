package com.vmware.action.review;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;

import static java.lang.String.format;

@ActionDescription("Uses rbt post to upload a changelist as a diff to reviewboard, only for perforce.")
public class UploadReviewDiffFromChangelist extends BaseLinkedPerforceCommitAction {

    public UploadReviewDiffFromChangelist(WorkflowConfig config) {
        super(config);
        super.addExpectedCommandsToBeAvailable("rbt");
        super.failIfCannotBeRun = true;
    }

    @Override
    public void process() {
        File clientDirectory = serviceLocator.getPerforce().getWorkingDirectory();
        String command = format("rbt post -r %s %s", draft.id, draft.perforceChangelistId);
        String output = CommandLineUtils.executeCommand(clientDirectory, command, null, false, LogLevel.INFO);
        if (!output.contains("Review request #" + draft.id + " posted")) {
            log.error("Failed to upload diff successfully\n{}", output);
        }
    }
}
