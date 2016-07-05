package com.vmware.action.base;

import com.vmware.Perforce;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

public abstract class BasePerforceCommitAction extends BaseCommitAction {

    protected Perforce perforce;

    public BasePerforceCommitAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(config.perforceClientName)) {
            return "config value perforceClientName not set, if using git, can be set by running git config git-p4.client clientName";
        }
        this.perforce = serviceLocator.getPerforce();
        if (perforce.getWorkingDirectory() == null) {
            return "no root directory found for perforce client " + config.perforceClientName;
        }
        return super.failWorkflowIfConditionNotMet();
    }

}
