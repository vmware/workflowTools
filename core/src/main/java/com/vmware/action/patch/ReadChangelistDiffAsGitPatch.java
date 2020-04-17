package com.vmware.action.patch;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Used to read a perforce changelist diff as a git patch")
public class ReadChangelistDiffAsGitPatch extends BasePerforceCommitAction {

    public ReadChangelistDiffAsGitPatch(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(StringUtils.isEmpty(draft.perforceChangelistId), "no perforce changelist id set");
    }

    @Override
    public void process() {
        draft.draftPatchData = perforce.diffChangelistInGitFormat(draft.perforceChangelistId, LogLevel.INFO);
    }
}
