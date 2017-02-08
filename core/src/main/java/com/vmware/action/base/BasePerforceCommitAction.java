package com.vmware.action.base;

import com.vmware.scm.Perforce;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

public abstract class BasePerforceCommitAction extends BaseCommitAction {

    protected Perforce perforce;

    public BasePerforceCommitAction(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        String reasonForFailing = perforceClientCanBeUsed();
        if (reasonForFailing != null) {
            return reasonForFailing;
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void preprocess() {
        this.perforce = serviceLocator.getPerforce();
    }
}
