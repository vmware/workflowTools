package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Uses git p4 submit to checkin a commit to the perforce depot.")
public class SubmitToDepot extends BaseAction {
    public SubmitToDepot(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isBlank(git.configValue("git-p4.skipsubmitedit"))) {
            throw new RuntimeException("Git config value git-p4.skipsubmitedit needs to be set to true, run [git config git-p4.skipsubmitedit true]");
        }
        git.submit();
    }
}
