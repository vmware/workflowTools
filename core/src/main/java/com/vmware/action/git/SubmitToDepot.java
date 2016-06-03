package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Uses git p4 submit to checkin a commit to the perforce depot")
public class SubmitToDepot extends BaseAction {
    public SubmitToDepot(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isBlank(git.configValue("git-p4.skipsubmitedit"))) {
            log.info("Run git command [git config git-p4.skipsubmitedit true] to skip opening a text editor while submitting");
        }
        git.submit();
    }
}
