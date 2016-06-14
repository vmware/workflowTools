package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a git commit --amend if changes are detected.")
public class AmendCommit extends BaseCommitAmendAction {

    public AmendCommit(WorkflowConfig config) {
        super(config, false, true);
    }

    @Override
    public void process() {
        git.amendCommit(updatedCommitText());
    }
}
