package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Exits if no changelist id is set for the commit.")
public class ExitIfCommitDoesNotHaveAChangelistId extends BaseCommitAction {
    public ExitIfCommitDoesNotHaveAChangelistId(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(draft.perforceChangelistId)) {
            exitWithMessage("changelist id is blank for this commit");
        }
    }
}
