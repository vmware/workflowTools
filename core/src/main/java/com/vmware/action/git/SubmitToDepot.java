package com.vmware.action.git;

import com.vmware.action.BaseAction;
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
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(git.configValue("git-p4.skipsubmitedit"))) {
            return "git config value git-p4.skipsubmitedit needs to be set to true, run [git config git-p4.skipsubmitedit true]";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        log.info("Submitting changes diffed against tracking branch {}", config.trackingBranchPath());
        git.submit(config.trackingBranchPath());
    }
}
