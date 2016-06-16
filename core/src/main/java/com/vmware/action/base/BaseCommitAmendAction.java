package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

/**
 * Common functionality for actions that amend a git commit.
 */
public abstract class BaseCommitAmendAction extends BaseCommitCreateAction {
    protected static final boolean INCLUDE_ALL_CHANGES = true;
    protected static final boolean DONT_INCLUDE_ALL_CHANGES = false;
    protected static final boolean INCLUDE_JOB_RESULTS = true;
    protected static final boolean EXCLUDE_JOB_RESULTS = false;

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
