package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ActionDescription("Attempts based on summary to match the current commit to a perforce changelist.")
public class SelectMatchingChangelist extends BasePerforceCommitAction {
    public SelectMatchingChangelist(WorkflowConfig config) {
        super(config);
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
        Map<String, ReviewRequestDraft> potentialMatches = new HashMap<>();
        for (String changelistId: changelists) {
            String changelistDescription = perforce.readChangelist(changelistId);
            ReviewRequestDraft potentialMatch = new ReviewRequestDraft(changelistDescription, commitConfig);
            potentialMatch.perforceChangelistId = changelistId;
            if (summaryMatches(potentialMatch) || reviewNumberMatches(potentialMatch)) {
                draft.perforceChangelistId = changelistId;
                return;
            }
            potentialMatches.put(changelistId, potentialMatch);
        }

        for (String changelistId : potentialMatches.keySet()) {
            ReviewRequestDraft potentialMatch = potentialMatches.get(changelistId);
            if (bugNumbersMatches(potentialMatch)) {
                draft.perforceChangelistId = changelistId;
                return;
            }
        }
    }

    private boolean summaryMatches(ReviewRequestDraft potentialMatch) {
        if (StringUtils.equals(draft.summary, potentialMatch.summary)) {
            log.info("Using changelist {} as summary matches commit summary", potentialMatch.perforceChangelistId);
            return true;
        } else {
            return false;
        }
    }

    private boolean reviewNumberMatches(ReviewRequestDraft potentialMatch) {
        if (StringUtils.isNotBlank(draft.id) && draft.id.equals(potentialMatch.id)) {
            log.info("Using changelist {} as review number {} matches commit", potentialMatch.perforceChangelistId, draft.id);
            return true;
        } else {
            return false;
        }
    }

    private boolean bugNumbersMatches(ReviewRequestDraft potentialMatch) {
        if (draft.hasBugNumber(commitConfig.noBugNumberLabel) && draft.bugNumbers.equals(potentialMatch.bugNumbers)) {
            log.info("Using changelist {} as bug numbers {} match commit", potentialMatch.perforceChangelistId, draft.bugNumbers);
            return true;
        } else {
            return false;
        }
    }
}
