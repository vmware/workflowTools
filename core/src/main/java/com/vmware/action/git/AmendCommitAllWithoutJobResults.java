package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a git commit --amend --all if changes are detected. Strips job results from commit text.")
public class AmendCommitAllWithoutJobResults extends BaseCommitAmendAction {
    public AmendCommitAllWithoutJobResults(WorkflowConfig config) {
        super(config, INCLUDE_ALL_CHANGES, EXCLUDE_JOB_RESULTS);
    }
}
