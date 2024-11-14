package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Performs a git fetch and git merge against the tracking branch")
public class MergeFromTrackingBranch extends BaseCommitAction {
    public MergeFromTrackingBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String trackingBranch = git.getTrackingBranch();
        log.info("Merging latest changes from {}", trackingBranch);
        git.fetch();
        git.merge(trackingBranch);
    }
}
