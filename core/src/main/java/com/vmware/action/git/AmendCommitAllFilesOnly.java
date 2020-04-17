package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Performs a git commit --amend --all without modifying any part of the commit message. Uses the existing commit message.")
public class AmendCommitAllFilesOnly extends BaseCommitAmendAction {

    public AmendCommitAllFilesOnly(WorkflowConfig config) {
        super(config, true, false);
    }

    @Override // always run
    public void checkIfActionShouldBeSkipped() {
    }

    @Override
    protected void commitUsingGit(String description) {
        String existingHeadRef = git.revParse("head");
        git.amendCommitWithAllFileChanges(git.lastCommitBody());
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, LogLevel.INFO);
    }

}
