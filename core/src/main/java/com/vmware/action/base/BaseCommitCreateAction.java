package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

/**
 * Common functionality for actions that create a git commit.
 */
public abstract class BaseCommitCreateAction extends BaseCommitAction {
    public BaseCommitCreateAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        failIfGitRepoOrPerforceClientCannotBeUsed();
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(!draft.hasData(), "there no information set for the commit message");
    }

    @Override
    public void process() {
        String description = draft.toText(commitConfig);
        if (git.workingDirectoryIsInGitRepo()) {
            commitUsingGit(description);
        } else if (StringUtils.isNotEmpty(perforceClientConfig.perforceClientName)) {
            commitUsingPerforce(description);
        }
    }

    protected void commitUsingGit(String description) {
        git.commit(draft.toText(commitConfig));
    }

    protected void commitUsingPerforce(String description) {
        log.info("Updating changelist description for {}", draft.perforceChangelistId);
        serviceLocator.getPerforce().updatePendingChangelist(draft.perforceChangelistId, description);
    }
}
