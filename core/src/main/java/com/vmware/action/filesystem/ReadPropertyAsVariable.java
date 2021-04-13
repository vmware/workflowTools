package com.vmware.action.filesystem;

import java.util.Properties;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;

@ActionDescription("Reads property from loaded file to the specified output variable.")
public class ReadPropertyAsVariable extends BaseAction {
    public ReadPropertyAsVariable(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData", "propertyName", "outputVariableName");
    }

    @Override
    public void process() {
        Properties properties = FileUtils.loadProperties(fileSystemConfig.fileData);

        String propertyValue = properties.getProperty(fileSystemConfig.propertyName);
        if (propertyValue == null) {
            log.info("Adding variable {} for property {} with empty string value", fileSystemConfig.outputVariableName, fileSystemConfig.propertyName);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, "", false);
        } else {
            log.info("Adding variable {} for property {} with value {}", fileSystemConfig.outputVariableName, fileSystemConfig.propertyName, propertyValue);
            replacementVariables.addVariable(fileSystemConfig.outputVariableName, propertyValue, false);
        }
    }
}
