package com.vmware.action.commitInfo;

import com.vmware.action.base.AbstractReadMultiLine;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Append text to the existing description.")
public class AddToDescription extends AbstractReadMultiLine {

    public AddToDescription(WorkflowConfig config) throws NoSuchFieldException {
        super(config, "description", true);
    }

}
