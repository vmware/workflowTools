package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.logging.Level;

@ActionDescription("Performs a git commit --amend --all without modifying any part of the commit message. Uses the existing commit message.")
public class AmendCommitFilesOnly extends BaseCommitAction {

    public AmendCommitFilesOnly(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String existingHeadRef = git.revParse("head");
        git.amendCommitWithAllFileChanges(git.lastCommitText(true));
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, Level.INFO);
    }

}
