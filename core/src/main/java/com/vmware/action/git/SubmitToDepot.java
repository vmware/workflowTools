package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Uses git p4 submit to checkin a commit to the perforce depot.")
public class SubmitToDepot extends BaseCommitAction {
    public SubmitToDepot(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        if (StringUtils.isEmpty(git.configValue("git-p4.skipsubmitedit"))) {
            exitDueToFailureCheck("git config value git-p4.skipsubmitedit needs to be set to true, run [git config git-p4.skipsubmitedit true]");
        }
    }

    @Override
    public void process() {
        log.info("Submitting changes diffed against tracking branch {}", gitRepoConfig.trackingBranchPath());
        git.submit(gitRepoConfig.trackingBranchPath());
    }
}
