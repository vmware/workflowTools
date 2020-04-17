package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

import static com.vmware.util.StringUtils.isNotEmpty;

@ActionDescription("Creates a new pending changelist in perforce if needed.")
public class CreatePendingChangelistIfNeeded extends BasePerforceCommitAction {

    public CreatePendingChangelistIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(isNotEmpty(draft.perforceChangelistId), "commit already linked with changelist " + draft.perforceChangelistId);
    }

    @Override
    public void process() {
        String changelistText = draft.toText(commitConfig);
        String changelistId = perforce.createPendingChangelist(changelistText, false);
        log.info("Created changelist with id {}", changelistId);
        draft.perforceChangelistId = changelistId;
        log.info("Adding tag changeset-{}", changelistId);
        if (git.workingDirectoryIsInGitRepo()) {
            git.updateTag("changeset-" + changelistId, LogLevel.DEBUG);
        }
    }
}
