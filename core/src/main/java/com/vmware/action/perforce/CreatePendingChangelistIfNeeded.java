package com.vmware.action.perforce;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Creates a new pending changelist in perforce if needed.")
public class CreatePendingChangelistIfNeeded extends BaseCommitAction {

    public CreatePendingChangelistIfNeeded(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("p4");
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isNotBlank(draft.perforceChangelistId)) {
            return "commit already associated with changelist " + draft.perforceChangelistId;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        String changelistText = draft.toGitText(config.getCommitConfiguration());
        String changelistId = perforce.createPendingChangelist(changelistText, false);
        log.info("Created changelist with id {}", changelistId);
        draft.perforceChangelistId = changelistId;
        log.info("Adding tag changeset-{}", changelistId);
        git.updateTag("changeset-" + changelistId, LogLevel.DEBUG);
    }
}
