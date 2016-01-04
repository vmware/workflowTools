package com.vmware.action.conditional;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if the current commit is reviewed by trivial.")
public class ExitIfTrivialCommit extends AbstractCommitAction {

    public ExitIfTrivialCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (!draft.isTrivialCommit(config.trivialReviewerLabel)) {
            return;
        }

        log.info("");
        log.info("Exiting as the commit is trivial");
        System.exit(0);
    }
}
