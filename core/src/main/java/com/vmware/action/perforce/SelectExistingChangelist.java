package com.vmware.action.perforce;

import com.vmware.action.base.BaseLinkedPerforceCommitAction;
import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

import java.util.List;

@ActionDescription("If only one changelist exists, that is selected by default. Otherwise the user is asked to select a changelist.")
public class SelectExistingChangelist extends BasePerforceCommitAction {
    public SelectExistingChangelist(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public String cannotRunAction() {
        if (!git.workingDirectoryIsInGitRepo()) {
            return "not in git repo directory";
        }
        if (StringUtils.isNotBlank(draft.perforceChangelistId)) {
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
