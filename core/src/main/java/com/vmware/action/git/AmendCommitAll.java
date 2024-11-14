package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAmendAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.LogLevel;

@ActionDescription("Performs a git commit --amend --all if changes are detected, --all will automatically add tracked file changes to the commit.")
public class AmendCommitAll extends BaseCommitAmendAction {

    public AmendCommitAll(WorkflowConfig config) {
        super(config, INCLUDE_ALL_CHANGES, INCLUDE_JOB_RESULTS);
    }

    @Override
    protected void commitUsingGit(String description) {
        String existingHeadRef = git.revParse("head");
        git.amendCommitAll(description, gitRepoConfig.noVerify);
        git.updateGitChangesetTagsMatchingRevision(existingHeadRef, LogLevel.INFO);
    }
}
