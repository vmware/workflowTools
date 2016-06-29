package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Updates the description for the pending changelist.")
public class UpdateChangelistDescription extends BasePerforceCommitAction {
    public UpdateChangelistDescription(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String description = draft.toGitText(config.getCommitConfiguration());
        String existingDescription = perforce.readChangelist(draft.perforceChangelistId);
        if (StringUtils.isBlank(existingDescription)) {
            log.error("No description read for changelist {}", draft.perforceChangelistId);
            return;
        } else if (!existingDescription.contains("\n")) {
            log.error("Not updating description for changelist {} as it has no new line characters\n{}",
                    draft.perforceChangelistId, existingDescription);
            return;
        }
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
