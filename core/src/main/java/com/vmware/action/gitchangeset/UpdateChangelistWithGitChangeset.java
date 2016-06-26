package com.vmware.action.gitchangeset;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Updates the git tag first, then runs git changeset update. Git changeset is a vmware internal tool.")
public class UpdateChangelistWithGitChangeset extends BasePerforceCommitAction {
    public UpdateChangelistWithGitChangeset(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        git.updateTag("changeset-" + draft.perforceChangelistId, LogLevel.DEBUG);
        git.changesetCommand("update", LogLevel.INFO);
    }
}
