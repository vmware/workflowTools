package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exits if the local git branch does not have a local commit.")
public class ExitIfBranchDoesNotHaveLocalCommit extends BaseAction {
    public ExitIfBranchDoesNotHaveLocalCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (!git.workingDirectoryIsInGitRepo()) {
            skipActionDueTo("directory not in git repo");
        }
    }

    @Override
    public void process() {
        String headRef = git.revParse("HEAD");
        String trackingBranchPath = gitRepoConfig.trackingBranchPath();
        String trackingBranchRef= git.revParse(trackingBranchPath);
        if (headRef.equals(trackingBranchRef)) {
            cancelWithMessage("local branch ref {} matches tracking branch {}. No local commit on branch.", headRef, trackingBranchPath);
        }
    }
}
