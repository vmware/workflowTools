package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

import java.util.List;

@ActionDescription("If only one changelist exists, that is selected by default. Otherwise the user is asked to select a changelist.")
public class SelectExistingChangelist extends BasePerforceCommitUsingGitAction {
    public SelectExistingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isNotEmpty(draft.perforceChangelistId)) {
            return "commit already is linked to changelist " + draft.perforceChangelistId;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        List<String> changelists = perforce.getPendingChangelists();

        if (changelists.isEmpty()) {
            log.info("No pending changelists in client {}", perforce.getClientName());
            return;
        }

        if (changelists.size() == 1) {
            log.info("Using changelist {} as matching changelist since only one changelist exists", changelists.get(0));
            draft.perforceChangelistId = changelists.get(0);
        } else {
            draft.perforceChangelistId = InputUtils.readValueUntilNotBlank("Select changelist to use", changelists);
        }
    }
}
