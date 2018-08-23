package com.vmware.action.patch;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Reads diff for git commit as git patch")
public class ReadGitDiffAsGitPatch extends BaseCommitAction {
    public ReadGitDiffAsGitPatch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        draft.draftPatchData = git.diff(gitRepoConfig.parentBranchPath(), "HEAD", true);
    }
}
