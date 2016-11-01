package com.vmware.action.perforce;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

@ActionDescription("Updates the description for the pending changelist matching the git commit.")
public class UpdateMatchingChangelistDescription extends BaseLinkedPerforceCommitAction {
    public UpdateMatchingChangelistDescription(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public void process() {
        String description = draft.toText(config.getCommitConfiguration());
        String existingPerforceChangelistText = perforce.readChangelist(draft.perforceChangelistId);
        ReviewRequestDraft existingDraft = new ReviewRequestDraft(existingPerforceChangelistText, config.getCommitConfiguration());
        String existingDescription = existingDraft.toText(config.getCommitConfiguration(), true);
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
