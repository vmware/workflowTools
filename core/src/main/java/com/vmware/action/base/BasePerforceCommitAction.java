package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

public abstract class BasePerforceCommitAction extends BaseCommitAction {

    public BasePerforceCommitAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(config.perforceClientName)) {
            return "config value perforceClientName not set, if using git, can be set by running git config git-p4.client clientName";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no changelist id read for commit";
        }
        return super.cannotRunAction();
    }
}
