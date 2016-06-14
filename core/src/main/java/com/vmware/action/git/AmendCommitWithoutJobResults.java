package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a git commit --amend if changes are detected. Strips job results from commit text.")
public class AmendCommitWithoutJobResults extends BaseCommitAmendAction {
    public AmendCommitWithoutJobResults(WorkflowConfig config) {
        super(config, false, false);
    }

    @Override
    public void process() {
        git.amendCommit(updatedCommitText());
    }
}
