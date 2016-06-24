package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

import java.util.List;

@ActionDescription("Attempts based on summary to match the current commit to a perforce changelist.")
public class SelectMatchingChangelist extends BaseCommitAction {
    public SelectMatchingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.perforceChangelistId != null) {
            return "commit already is linked to changelist " + draft.perforceChangelistId;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        draft.perforceChangelistId = null;
        List<String> changelists = perforce.getPendingChangelists(config.perforceClientName);
        for (String changelist: changelists) {
            if (changelistDescriptionMatches(changelist)) {
                log.info("Using changelist {} as summary matches commit summary", changelist);
                draft.perforceChangelistId = changelist;
                break;
            }
        }
        if (draft.perforceChangelistId == null) {
            log.info("No changelist summary matches commit summary {}", draft.summary);
            if (changelists.size() == 1) {
                log.info("Using changelist {} as matching changelist since only one changelist exists", changelists.get(0));
                draft.perforceChangelistId = changelists.get(0);
            }
        }
    }

    private boolean changelistDescriptionMatches(String changelist) {
        String description = perforce.readChangelist(changelist);
        String descriptionWithoutChangelistLine = description.substring(description.indexOf('\n')).trim();
        ReviewRequestDraft matchingDraft = new ReviewRequestDraft();
        matchingDraft.fillValuesFromCommitText(descriptionWithoutChangelistLine, config.getCommitConfiguration());
        return StringUtils.equals(draft.summary, matchingDraft.summary);
    }
}
