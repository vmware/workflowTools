package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

import java.util.List;

@ActionDescription("Selects the existing changelist if only one changelist exists.")
public class SelectExistingChangelist extends BasePerforceCommitAction {
    public SelectExistingChangelist(WorkflowConfig config) {
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

        if (changelists.size() == 1) {
            log.info("Using changelist {} as matching changelist since only one changelist exists", changelists.get(0));
            draft.perforceChangelistId = changelists.get(0);
        }
    }
}
