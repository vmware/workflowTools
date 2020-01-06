package com.vmware.action.base;

import com.vmware.util.scm.Perforce;
import com.vmware.config.WorkflowConfig;

public abstract class BasePerforceCommitAction extends BaseCommitAction {

    protected Perforce perforce;

    public BasePerforceCommitAction(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
        super.failIfCannotBeRun = true;
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        String reasonForFailing = perforceClientCannotBeUsed();
        if (reasonForFailing != null) {
            exitDueToFailureCheck(reasonForFailing);
        }
    }

    @Override
    public void preprocess() {
        this.perforce = serviceLocator.getPerforce();
    }
}
