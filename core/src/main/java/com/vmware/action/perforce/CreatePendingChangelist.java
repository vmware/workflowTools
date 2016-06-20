package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Creates a new pending changelist in perforce.")
public class CreatePendingChangelist extends BaseCommitAction {

    public CreatePendingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistText = draft.toGitText(config.getCommitConfiguration());
        String changelistId = perforce.createPendingChangelist(changelistText, false);
        log.info("Created changelist with id {}", changelistId);
    }
}
