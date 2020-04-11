package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Simple action to print a newline, used for formatting output.")
public class DisplayLineBreak extends BaseAction {
    public DisplayLineBreak(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("");
    }
}
