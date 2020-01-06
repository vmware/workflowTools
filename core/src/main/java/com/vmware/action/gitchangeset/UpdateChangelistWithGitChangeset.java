package com.vmware.action.gitchangeset;

import com.vmware.action.base.BaseLinkedPerforceCommitUsingGitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Updates the git tag first, then runs git changeset update. Git changeset is a vmware internal tool.")
public class UpdateChangelistWithGitChangeset extends BaseLinkedPerforceCommitUsingGitAction {
    public UpdateChangelistWithGitChangeset(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(git.configValue("changesetsync.checkoutdir"))) {
            throw new RuntimeException("git changeset has not been setup for this repo, git config value changesetsync.checkoutdir is missing");
        }
        git.updateTag("changeset-" + draft.perforceChangelistId, LogLevel.DEBUG);
        git.changesetCommand("update", LogLevel.INFO);
    }
}
