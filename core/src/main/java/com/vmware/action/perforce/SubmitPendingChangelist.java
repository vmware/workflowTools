package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Submits the perforce changelist to the depot.")
public class SubmitPendingChangelist extends BasePerforceCommitAction {
    public SubmitPendingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(StringUtils.isEmpty(draft.perforceChangelistId), "no changelist specified");
    }

    @Override
    public void process() {
        final boolean DO_NOT_INCLUDE_JOB_RESULTS = false;
        String description = draft.toText(commitConfig, DO_NOT_INCLUDE_JOB_RESULTS);
        perforce.submitChangelist(draft.perforceChangelistId, description);
    }
}
