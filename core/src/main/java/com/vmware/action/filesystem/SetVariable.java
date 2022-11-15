package com.vmware.action.filesystem;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Set a value for a variable")
public class SetVariable extends BaseAction {
    public SetVariable(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("variable", "inputText");
    }

    @Override
    public void process() {
        log.info("Setting variable {} to {}", fileSystemConfig.variable, fileSystemConfig.inputText);
        replacementVariables.addVariable(fileSystemConfig.variable, fileSystemConfig.inputText);
    }
}
