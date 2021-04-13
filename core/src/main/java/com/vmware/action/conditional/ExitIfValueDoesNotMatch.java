package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exits if the specified value does not match the supplied value.")
public class ExitIfValueDoesNotMatch extends BaseAction {
    public ExitIfValueDoesNotMatch(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("inputText", "propertyValue");
    }

    @Override
    public void process() {
        log.debug("Comparing value {} with expected value {}", fileSystemConfig.inputText, fileSystemConfig.propertyValue);
        if (!fileSystemConfig.inputText.equals(fileSystemConfig.propertyValue)) {
            cancelWithMessage(fileSystemConfig.inputText + " does not match " + fileSystemConfig.propertyValue);
        }
    }
}
