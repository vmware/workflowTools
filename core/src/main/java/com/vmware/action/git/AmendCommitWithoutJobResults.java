package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Performs a git commit --amend if changes are detected. Strips job results from commit text.")
public class AmendCommitWithoutJobResults extends BaseCommitAmendAction {
    public AmendCommitWithoutJobResults(WorkflowConfig config) {
        super(config, DONT_INCLUDE_ALL_CHANGES, EXCLUDE_JOB_RESULTS);
    }
}
