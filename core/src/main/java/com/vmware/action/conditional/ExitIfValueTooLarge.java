package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exits if the specified value is too large.")
public class ExitIfValueTooLarge extends BaseAction {
    public ExitIfValueTooLarge(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("inputText", "propertyValue");
    }

    @Override
    public void process() {
        log.debug("Checking if value {}'s length of {} is larger than {}",
                fileSystemConfig.inputText, fileSystemConfig.inputText.length(), fileSystemConfig.propertyValue);
        if (fileSystemConfig.inputText.length() > Integer.parseInt(fileSystemConfig.propertyValue)) {
            cancelWithMessage(fileSystemConfig.inputText + " is larger than " + fileSystemConfig.propertyValue);
        }
    }
}
