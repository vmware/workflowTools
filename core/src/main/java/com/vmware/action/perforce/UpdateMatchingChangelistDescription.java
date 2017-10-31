package com.vmware.action.perforce;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;

@ActionDescription("Updates the description for the pending changelist matching the git commit.")
public class UpdateMatchingChangelistDescription extends BaseLinkedPerforceCommitAction {
    public UpdateMatchingChangelistDescription(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String description = draft.toText(commitConfig);
        String existingPerforceChangelistText = perforce.readChangelist(draft.perforceChangelistId);
        ReviewRequestDraft existingDraft = new ReviewRequestDraft(existingPerforceChangelistText, commitConfig);
        String existingDescription = existingDraft.toText(commitConfig, true);
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
