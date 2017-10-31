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
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(commitConfig.defaultApprover)) {
            return "no default approver set, config value defaultApprover must be set";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        log.info("Setting approved by to default approver value {}", commitConfig.defaultApprover);
        draft.approvedBy = commitConfig.defaultApprover;
    }
}
