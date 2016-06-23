package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Creates a new pending changelist in perforce if needed.")
public class CreatePendingChangelistIfNeeded extends BaseCommitAction {

    public CreatePendingChangelistIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.perforceChangelistId != null) {
            return "commit already associated with changelist " + draft.perforceChangelistId;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        String changelistText = draft.toGitText(config.getCommitConfiguration());
        String changelistId = perforce.createPendingChangelist(changelistText, false);
        log.info("Created changelist with id {}", changelistId);
        draft.perforceChangelistId = changelistId;
    }
}
