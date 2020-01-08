package com.vmware.action.conditional;

import com.vmware.action.base.BaseIssuesProcessingAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Helper action for exiting if there are no project issues to process.")
public class ExitIfThereAreNoIssuesToProcess extends BaseIssuesProcessingAction {

    public ExitIfThereAreNoIssuesToProcess(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (projectIssues.noIssuesAdded()) {
            cancelWithMessage("no issues to process");
        }
    }
}
