package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Pushed to the remote branch that this local branch is tracking.")
public class PushToTrackingBranch extends BaseAction {

    public PushToTrackingBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String trackingBranch = git.getTrackingBranch();
        if (StringUtils.isEmpty(trackingBranch)) {
            log.debug("Branch {} does not track a remote branch, using configured tracking branch {}",
                    git.currentBranch(), gitRepoConfig.trackingBranchPath());
            trackingBranch = gitRepoConfig.trackingBranchPath();
        }

        String[] pieces = trackingBranch.split("/");
        if (pieces.length != 2) {
            cancelWithErrorMessage("Expected tracking branch to be of the format remote/branchName, was " + trackingBranch);
        }
        String remote = pieces[0];
        String branch = pieces[1];
        git.pushToRemoteBranch(remote, branch);
    }
}
