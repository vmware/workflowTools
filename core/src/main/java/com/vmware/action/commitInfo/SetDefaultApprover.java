package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Set approved by to the default user value.")
public class SetDefaultApprover extends BaseCommitAction {
    public SetDefaultApprover(WorkflowConfig config) {
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
        log.info("Setting approved by to default approver value {}", commitConfig.approver);
        draft.approvedBy = commitConfig.approver;
    }
}
