package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

/**
 * Common functionality for actions that amend a commit.
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
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(commitHasNoChanges(), "no changes detected");
    }

    @Override
    protected void commitUsingGit(String description) {
        String existingHeadRef = git.revParse("head");
        git.amendCommit(updatedCommitText(includeJobResultsInCommit));
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, LogLevel.INFO);
    }

    private boolean commitHasNoChanges() {
        if (!commitTextHasNoChanges(includeJobResultsInCommit)) {
            return false;
        }

        if (!git.workingDirectoryIsInGitRepo()) {
            return true;
        }

        if (git.getAllChanges().isEmpty()) {
            return true;
        }

        return !includeAllChangesInCommit || git.getStagedChanges().isEmpty();
    }
}
