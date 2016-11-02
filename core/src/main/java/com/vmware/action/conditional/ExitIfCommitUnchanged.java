package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if the commit details in memory are not different to the last commit.")
public class ExitIfCommitUnchanged extends BaseCommitAction {
    public ExitIfCommitUnchanged(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String existingCommitText = git.lastCommitText(true);
        String updatedCommitText = draft.toText(config.getCommitConfiguration());

        if (!existingCommitText.equals(updatedCommitText)) {
            return;
        }

        log.info("");
        log.info("Commit does not have any changes, exiting");
        System.exit(0);
    }
}
