package com.vmware.action.slack;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Posts an approval request message to the approve in Slack.")
public class PostApprovalRequest extends BaseCommitAction {
    public PostApprovalRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        if (StringUtils.isEmpty(commitConfig.approver)) {
            exitDueToFailureCheck("no approver set, config value approver must be set");
        }
    }

    @Override
    public void process() {
        String confirmation = InputUtils.readValue("Send approval request to " + commitConfig.approver + ": Type yes to confirm");
        if (!confirmation.equalsIgnoreCase("yes")) {
            log.info("Canceling sending of approval request");
        }

    }
}
