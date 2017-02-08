package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;

import static java.lang.String.format;

@ActionDescription("Uses rbt post to upload a changelist as a diff to reviewboard, only for perforce.")
public class UploadReviewDiffFromChangelist extends BaseCommitWithReviewAction {

    public UploadReviewDiffFromChangelist(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("rbt", "p4");
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        String reasonForFailing = perforceClientCanBeUsed();
        if (StringUtils.isNotBlank(reasonForFailing)) {
            return reasonForFailing;
        }
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no matching changelist found, run createPendingChangelist as part of workflow";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        File clientDirectory = serviceLocator.getPerforce().getWorkingDirectory();
        String command = format("rbt post -r %s %s", draft.id, draft.perforceChangelistId);
        String output = CommandLineUtils.executeCommand(clientDirectory, command, null, LogLevel.INFO);
        if (!output.contains("Review request #" + draft.id + " posted")) {
            log.error("Failed to upload diff successfully\n{}", output);
        }
    }
}
