package com.vmware.action.perforce;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Moves all open files to the specified pending changelist.")
public class MoveOpenFilesToPendingChangelist extends BaseLinkedPerforceCommitAction {
    public MoveOpenFilesToPendingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Moving all open files to changelist {}", draft.perforceChangelistId);
        perforce.reopenAllOpenFilesInChangelist(draft.perforceChangelistId);
    }
}
