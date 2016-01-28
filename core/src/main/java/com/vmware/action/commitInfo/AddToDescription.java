package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseReadMultiLine;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Append text to the existing description.")
public class AddToDescription extends BaseReadMultiLine {

    public AddToDescription(WorkflowConfig config) {
        super(config, "description", true);
    }

}
