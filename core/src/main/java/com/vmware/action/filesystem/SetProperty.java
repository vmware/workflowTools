package com.vmware.action.filesystem;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;

@ActionDescription("Adds or replaces a property value.")
public class SetProperty extends BaseAction {
    public SetProperty(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("fileData", "propertyName", "propertyValue");
    }

    @Override
    public void process() {
        String propertyValue = fileSystemConfig.propertyValue;

        Properties properties = FileUtils.loadProperties(fileSystemConfig.fileData);

        String existingPropertyValue = properties.getProperty(fileSystemConfig.propertyName);
        if (StringUtils.isEmpty(existingPropertyValue)) {
            log.info("Adding property named {} with value {}", fileSystemConfig.propertyName, propertyValue);
        } else {
            log.info("Replacing property value {} for {} with {}", existingPropertyValue, fileSystemConfig.propertyName, propertyValue);
        }
        properties.setProperty(fileSystemConfig.propertyName, propertyValue);
        storeProperties(properties);
    }



    private void storeProperties(Properties properties) {
        StringBuilder builder = new StringBuilder();
        List<String> sortedNames = properties.stringPropertyNames().stream().sorted().collect(Collectors.toList());

        for (String propertyName : sortedNames) {
            builder.append(propertyName).append("=").append(properties.getProperty(propertyName)).append("\n");
        }
        fileSystemConfig.fileData = builder.toString();
    }
}
