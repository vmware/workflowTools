package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Uses git p4 submit to checkin a commit to the perforce depot")
public class SubmitToDepot extends BaseAction {
    public SubmitToDepot(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        git.submit();
    }
}
