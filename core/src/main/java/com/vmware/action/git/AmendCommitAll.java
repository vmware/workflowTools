package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.logging.Level;

@ActionDescription("Performs a git commit --amend --all if changes are detected, --all will automatically add tracked file changes to the commit.")
public class AmendCommitAll extends BaseCommitAmendAction {

    public AmendCommitAll(WorkflowConfig config) {
        super(config, INCLUDE_ALL_CHANGES, INCLUDE_JOB_RESULTS);
    }

    @Override
    public void process() {
        String existingHeadRef = git.revParse("head");
        git.amendCommitWithAllFileChanges(updatedCommitText());
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, Level.INFO);
    }
}
