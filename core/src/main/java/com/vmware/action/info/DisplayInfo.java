package com.vmware.action.info;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Simple action for displaying specified information")
public class DisplayInfo extends BaseAction {
    public DisplayInfo(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("inputText");
    }

    @Override
    public void process() {
        log.info(fileSystemConfig.inputText);
    }
}
