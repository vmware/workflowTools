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
        if (StringUtils.isBlank(config.perforceClientName)) {
            return "config value perforceClientName not set, if using git, can be set by running git config git-p4.client clientName";
        }
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no matching changelist found, run createPendingChangelist as part of workflow";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        File clientDirectory = perforce.getClientDirectory(config.perforceClientName);
        if (clientDirectory == null) {
            throw new IllegalArgumentException("No root directory found for client " + config.perforceClientName + ", run p4 clients and check your client is present");
        }
        String command = format("rbt post -r %s %s", draft.id, draft.perforceChangelistId);
        String output = CommandLineUtils.executeCommand(clientDirectory, command, null, LogLevel.INFO);
        if (!output.contains("Review request #" + draft.id + " posted")) {
            log.error("Failed to upload diff successfully\n{}", output);
        }
    }
}
