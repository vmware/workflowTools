package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exits if the specified value matches the supplied value.")
public class ExitIfValueMatches extends BaseAction {
    public ExitIfValueMatches(WorkflowConfig config) {
        super(config);
        super.addSkipActionIfBlankProperties("inputText", "propertyValue");
    }

    @Override
    public void process() {
        log.debug("Comparing value {} with value {}", fileSystemConfig.inputText, fileSystemConfig.propertyValue);
        if (fileSystemConfig.inputText.equals(fileSystemConfig.propertyValue)) {
            cancelWithMessage(fileSystemConfig.inputText + " matches " + fileSystemConfig.propertyValue);
        }
    }
}
