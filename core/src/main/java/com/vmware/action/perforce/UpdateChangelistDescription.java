package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

@ActionDescription("Updates the description for the pending changelist.")
public class UpdateChangelistDescription extends BasePerforceCommitAction {
    public UpdateChangelistDescription(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public void process() {
        String description = draft.toGitText(config.getCommitConfiguration());
        String existingPerforceChangelistText = perforce.readChangelist(draft.perforceChangelistId);
        ReviewRequestDraft existingDraft = new ReviewRequestDraft(existingPerforceChangelistText, config.getCommitConfiguration());
        if (StringUtils.isBlank(existingDraft.description)) {
            log.error("No description read for changelist {}", draft.perforceChangelistId);
            return;
        }
        if (description.equals(existingDraft.description)) {
            log.info("Not updating description for changelist {} as it hasn't changed", draft.perforceChangelistId);
            return;
        }
        boolean success = perforce.updatePendingChangelist(draft.perforceChangelistId, description);
        if (success) {
            log.info("Updated description for changelist {}", draft.perforceChangelistId);
        }
    }
}
