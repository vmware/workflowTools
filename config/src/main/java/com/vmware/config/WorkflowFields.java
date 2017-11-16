package com.vmware.config;

import com.google.gson.annotations.Expose;
import com.vmware.config.section.SectionConfig;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Encapsulates a list of workflow fields.
 */
public class WorkflowFields {

    @Expose(serialize = false, deserialize = false)
    private Map<String, String> overriddenConfigSources = new TreeMap<>();

    @Expose(serialize = false, deserialize = false)
    private List<WorkflowField> configurableFields = new ArrayList<>();

    private WorkflowConfig workflowConfig;

    public WorkflowFields(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
        populateConfigurableFields();
    }

    public List<ConfigurableProperty> applyConfigValues(Map<String, String> configValues, String source) {
        List<ConfigurableProperty> propertiesAffected = new ArrayList<>();
        for (WorkflowField field : configurableFields) {
            ConfigurableProperty configurableProperty = field.configAnnotation();
            if (configurableProperty.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }
            for (String configValue : configValues.keySet()) {
                List<String> commandLineArguments = StringUtils.splitAndTrim(configurableProperty.commandLine(), ",");
                if (commandLineArguments.contains(configValue)) {
                    propertiesAffected.add(configurableProperty);
                    String value = configValues.get(configValue);
                    if (value == null && (field.getType() == Boolean.class || field.getType() == boolean.class)) {
                        value = Boolean.TRUE.toString();
                    }
                    setFieldValue(field, value, source);
                }
            }
        }
        return propertiesAffected;
    }

    public void overrideValues(WorkflowConfig overriddenConfig, String configFileName) {
        for (WorkflowField field : configurableFields) {
            Object existingValue = field.getValue(workflowConfig);
            Object value = field.getValue(overriddenConfig);
            if (value == null || String.valueOf(value).equals("0") || (value instanceof Boolean && !((Boolean) value))) {
                continue;
            }
            // copy values to default config map if value is a map
            if (existingValue != null && value instanceof Map) {
                Map valueMap = (Map) value;
                if (valueMap.isEmpty()) {
                    continue;
                }
                Map existingValues = (Map) existingValue;
                String existingConfigValue = overriddenConfigSources.get(field.getName());
                String updatedConfigValue;
                if (existingConfigValue == null && !existingValues.isEmpty()) {
                    updatedConfigValue = "default, " + configFileName;
                } else if (existingConfigValue == null) {
                    updatedConfigValue = configFileName;
                } else {
                    updatedConfigValue = existingConfigValue + ", " + configFileName;
                }
                overriddenConfigSources.put(field.getName(), updatedConfigValue);
                existingValues.putAll(valueMap);
            } else {
                overriddenConfigSources.put(field.getName(), configFileName);
                // override for everything else
                field.setValue(workflowConfig, value);
            }
        }
    }

    public void applyGitConfigValues(String configPrefix, Map<String, String> gitConfigValues) {
        if (gitConfigValues.isEmpty()) {
            return;
        }
        String configPrefixText = StringUtils.isBlank(configPrefix) ? "" : configPrefix + ".";
        String sourceConfigProperty;
        for (WorkflowField field : configurableFields) {
            ConfigurableProperty configurableProperty = field.configAnnotation();
            String workflowConfigPropertyName = "workflow." + configPrefixText + field.getName().toLowerCase();
            String gitConfigPropertyName = configurableProperty.gitConfigProperty();
            String valueToSet;
            if (!gitConfigPropertyName.isEmpty() && StringUtils.isBlank(configPrefix)) {
                String valueByGitConfig = gitConfigValues.get(gitConfigPropertyName);
                String valueByWorkflowProperty = gitConfigValues.get(workflowConfigPropertyName);
                if (valueByGitConfig != null && valueByWorkflowProperty != null && !valueByGitConfig.equals(valueByWorkflowProperty)) {
                    throw new FatalException("Property {} has value {} specified by the git config property {}" +
                            " but has value {} specified by the workflow property {}, please remove one of the properties",
                            field.getName(), valueByGitConfig, gitConfigPropertyName, valueByWorkflowProperty,
                            workflowConfigPropertyName);
                }
                sourceConfigProperty = valueByGitConfig != null ? gitConfigPropertyName : workflowConfigPropertyName;
                valueToSet = valueByGitConfig != null ? valueByGitConfig : valueByWorkflowProperty;
            } else {
                sourceConfigProperty = workflowConfigPropertyName;
                valueToSet = gitConfigValues.get(workflowConfigPropertyName);
            }

            setFieldValue(field, valueToSet, "Git " + sourceConfigProperty);
        }
    }

    public WorkflowField getMatchingField(String commandLineProperty) {
        for (WorkflowField field : configurableFields) {
            ConfigurableProperty property = field.configAnnotation();
            if (property.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }

            List<String> params = Arrays.stream(property.commandLine().split(","))
                    .map(String::trim).collect(Collectors.toList());
            if (params.contains(commandLineProperty)) {
                return field;
            }
        }
        return null;
    }

    public String getFieldValueSource(String fieldName) {
        String value = overriddenConfigSources.get(fieldName);
        return value != null ? value : "default";
    }

    public void markFieldAsOverridden(String fieldName, String source) {
        overriddenConfigSources.put(fieldName, source);
    }

    public void setFieldValue(String fieldName, String value, String source) {
        WorkflowField matchingField = configurableFields.stream()
                .filter(field -> field.getName().equals(fieldName)).findFirst()
                .orElseThrow(() -> new FatalException("No configurable field found matching name " + fieldName));
        setFieldValue(matchingField, value, source);
    }

    public void setFieldValue(WorkflowField field, String value, String source) {
        Object validValue = field.determineValue(value);
        if (validValue != null) {
            overriddenConfigSources.put(field.getName(), source);
            field.setValue(workflowConfig, validValue);
        }
    }

    public List<WorkflowField> values() {
        return Collections.unmodifiableList(configurableFields);
    }

    public int size() {
        return configurableFields.size();
    }

    public WorkflowField get(int index) {
        return configurableFields.get(index);
    }



    private void addConfigurablePropertiesForClass(Class classToCheck, Map<String, Field> usedParams) {
        for (Field field : classToCheck.getFields()) {
            ConfigurableProperty configProperty = field.getAnnotation(ConfigurableProperty.class);
            if (configProperty == null) {
                continue;
            }
            String[] params = configProperty.commandLine().split(",");
            for (String param : params) {
                if (param.equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                    continue;
                }
                boolean fieldNameAlreadyUsed = configurableFields.stream()
                        .anyMatch(existingField -> existingField.getName().equals(field.getName()));
                if (!fieldNameAlreadyUsed && usedParams.containsKey(param)) {
                    throw new FatalException(
                            "Config flag {} has already been set to configure another property {}", param,
                            usedParams.get(param).getName());
                }
                usedParams.put(param, field);
            }
            configurableFields.add(new WorkflowField(field));
        }
    }

    private void populateConfigurableFields() {
        Map<String, Field> usedParams = new HashMap<>();
        addConfigurablePropertiesForClass(WorkflowConfig.class, usedParams);

        Arrays.stream(WorkflowConfig.class.getFields()).filter(field -> field.getAnnotation(SectionConfig.class) != null)
                .forEach(field -> addConfigurablePropertiesForClass(field.getType(), usedParams));
    }

}
