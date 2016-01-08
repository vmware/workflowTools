package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseReadMultiLine;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Sets the description section. Replaces existing value if there is one.")
public class SetDescription extends BaseReadMultiLine {
    public SetDescription(WorkflowConfig config) throws NoSuchFieldException {
        super(config, "description", false);
    }
}
