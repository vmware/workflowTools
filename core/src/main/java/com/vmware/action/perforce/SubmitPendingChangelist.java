package com.vmware.action.perforce;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Submits the perforce changelist to the depot.")
public class SubmitPendingChangelist extends BaseLinkedPerforceCommitAction {
    public SubmitPendingChangelist(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public void process() {
        final boolean DO_NOT_INCLUDE_JOB_RESULTS = false;
        String description = draft.toGitText(config.getCommitConfiguration(), DO_NOT_INCLUDE_JOB_RESULTS);
        perforce.submitChangelist(draft.perforceChangelistId, description);
    }
}
