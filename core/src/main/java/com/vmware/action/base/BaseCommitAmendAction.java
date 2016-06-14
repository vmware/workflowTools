package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

/**
 * Common functionality for actions that amend a git commit.
 */
public abstract class BaseCommitAmendAction extends BaseCommitCreateAction {
    private final boolean includeJobResultsInCommit;
    private final boolean includeAllChangesInCommit;

    public BaseCommitAmendAction(WorkflowConfig config, boolean includeAllChangesInCommit,
                                 boolean includeJobResultsInCommit) {
        super(config);
        this.includeAllChangesInCommit = includeAllChangesInCommit;
        this.includeJobResultsInCommit = includeJobResultsInCommit;
    }

    @Override
    public String cannotRunAction() {
        if (commitHasNoChanges()) {
            return "commit does not have any changes";
        }
        return super.cannotRunAction();
    }

    protected boolean commitHasNoChanges() {
        String existingCommitText = git.lastCommitText(true).trim();
        String updatedCommitText = updatedCommitText();

        if (!existingCommitText.equals(updatedCommitText)) {
            return false;
        }

        if (git.getAllChanges().isEmpty()) {
            return true;
        }

        return git.getStagedChanges().isEmpty() && includeAllChangesInCommit;
    }

    protected String updatedCommitText() {
        return draft.toGitText(config.getCommitConfiguration(), includeJobResultsInCommit).trim();
    }
}
