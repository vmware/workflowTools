package com.vmware.action.perforce;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

import java.util.List;

@ActionDescription("Attempts based on summary to match the current commit to a perforce changelist.")
public class SelectMatchingChangelist extends BaseLinkedPerforceCommitAction {
    public SelectMatchingChangelist(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isNotBlank(draft.perforceChangelistId)) {
            return "commit already is linked to changelist " + draft.perforceChangelistId;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        List<String> changelists = perforce.getPendingChangelists();
        for (String changelist: changelists) {
            if (changelistDescriptionMatches(changelist)) {
                log.info("Using changelist {} as summary matches commit summary", changelist);
                draft.perforceChangelistId = changelist;
                break;
            }
        }
    }

    private boolean changelistDescriptionMatches(String changelist) {
        String description = perforce.readChangelist(changelist);
        ReviewRequestDraft matchingDraft = new ReviewRequestDraft(description, config.getCommitConfiguration());
        return StringUtils.equals(draft.summary, matchingDraft.summary);
    }
}
