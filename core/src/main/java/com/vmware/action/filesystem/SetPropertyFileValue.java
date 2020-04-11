package com.vmware.action.filesystem;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeIOException;

@ActionDescription("Adds or replaces a property value in a specified property file.")
public class SetPropertyFileValue extends BaseAction {
    public SetPropertyFileValue(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("propertyFile", "propertyName", "propertyValue");
    }

    @Override
    public void process() {
        String propertyFilePath = git.addRepoDirectoryIfNeeded(fileSystemConfig.propertyFile);
        String propertyValue = git.addRepoDirectoryIfNeeded(fileSystemConfig.propertyValue);

        log.debug("Configured source file {}", propertyFilePath);

        Properties properties = loadProperties(propertyFilePath);

        String existingPropertyValue = properties.getProperty(fileSystemConfig.propertyName);
        if (StringUtils.isEmpty(existingPropertyValue)) {
            log.info("Adding property named {} with value {} to property file {}", fileSystemConfig.propertyName, propertyValue, propertyFilePath);
        } else {
            log.info("Replacing property value {} for {} with {} in {}", existingPropertyValue, fileSystemConfig.propertyName, propertyValue, propertyFilePath);
        }
        properties.setProperty(fileSystemConfig.propertyName, propertyValue);
        storeProperties(properties, propertyFilePath);
    }

    private Properties loadProperties(String sourceFilePath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(sourceFilePath));
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        return properties;
    }

    private void storeProperties(Properties properties, String sourceFilePath) {
        List<String> sortedNames = properties.stringPropertyNames().stream().sorted().collect(Collectors.toList());
        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(sourceFilePath, false))) {
            for (String propertyName : sortedNames) {
                fileWriter.write(propertyName + "=" + properties.getProperty(propertyName));
                fileWriter.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}
