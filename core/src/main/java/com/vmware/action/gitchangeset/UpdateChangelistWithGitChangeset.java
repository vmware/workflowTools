package com.vmware.action.gitchangeset;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.logging.Level;

@ActionDescription("Updates the git tag first, then runs git changeset update. Git changeset is a vmware internal tool.")
public class UpdateChangelistWithGitChangeset extends BasePerforceCommitAction {
    public UpdateChangelistWithGitChangeset(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        git.updateTag("changeset-" + draft.perforceChangelistId, Level.FINE);
        git.changesetCommand("update", Level.INFO);
    }
}
