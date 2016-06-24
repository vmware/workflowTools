package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Updates the description for the pending changelist.")
public class UpdateChangelistDescription extends BasePerforceCommitAction {
    public UpdateChangelistDescription(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String description = draft.toGitText(config.getCommitConfiguration());
        String existingDescription = perforce.readChangelist(draft.perforceChangelistId);
        existingDescription = existingDescription.substring(existingDescription.indexOf('\n')).trim();
        if (description.equals(existingDescription)) {
            log.info("Not updating description for changelist {} as it hasn't changed", draft.perforceChangelistId);
            return;
        }
        boolean success = perforce.updatePendingChangelist(draft.perforceChangelistId, description);
        if (success) {
            log.info("Updated description for changelist {}", draft.perforceChangelistId);
        }
    }
}
