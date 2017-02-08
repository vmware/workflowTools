package com.vmware.action.git;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Applies the diff for a changelist to a git branch.")
public class ApplyChangelistAsGitDiff extends BasePerforceCommitAction {

    public ApplyChangelistAsGitDiff(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no perforce changelist id set";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        String perforceDiff = perforce.diffChangelistInGitFormat(draft.perforceChangelistId, true, LogLevel.INFO);
        String checkData = git.applyDiff(perforceDiff, true);
        if (StringUtils.isNotBlank(checkData)) {
            throw new RuntimeException("Diff does not apply cleanly: " + checkData);
        }
        String output = git.applyDiff(perforceDiff, false);
        if (StringUtils.isNotBlank(output)) {
            log.info("Output from running git diff: {}", output);
        } else {
            log.info("Git diff cleanly applied");
        }
    }
}
