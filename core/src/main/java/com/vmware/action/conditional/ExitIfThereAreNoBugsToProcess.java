package com.vmware.action.conditional;

import com.vmware.action.base.BaseIssuesProcessingAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Helper action for exiting if there are no bugs to process.")
public class ExitIfThereAreNoBugsToProcess extends BaseIssuesProcessingAction {

    public ExitIfThereAreNoBugsToProcess(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (projectIssues.noBugsAdded()) {
            cancelWithMessage("no bugs to process");
        }
    }
}
