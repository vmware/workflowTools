package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Exit if variable is unset or empty")
public class ExitIfVariableIsUnset extends BaseAction {
    public ExitIfVariableIsUnset(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("variable");
    }

    @Override
    public void process() {
        if (!replacementVariables.hasVariable(fileSystemConfig.variable)) {
            cancelWithMessage(fileSystemConfig.variable + " is unset");
        }
        if (StringUtils.isEmpty(replacementVariables.getVariable(fileSystemConfig.variable))) {
            cancelWithMessage(fileSystemConfig.variable + " is blank");
        }
    }
}
