package com.vmware.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.config.section.SectionConfig;
import com.vmware.util.ArrayUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import static com.vmware.util.StringUtils.pluralizeDescription;

/**
 * Encapsulates a list of workflow fields.
 */
public class WorkflowFields {

    private static final String[] ADDITIONAL_ARGUMENT_NAMES = new String[] {"-c", "--config", "--possible-workflow", "--cancel-message"};

    @Expose(serialize = false, deserialize = false)
    private Map<String, String> overriddenConfigSources = new TreeMap<>();

    @Expose(serialize = false, deserialize = false)
    private List<WorkflowField> configurableFields = new ArrayList<>();

    private final List<String> loadedConfigFiles = new ArrayList<>();

    private final WorkflowConfig workflowConfig;

    public static boolean isSystemProperty(String propertyName) {
        if (ArrayUtils.contains(ADDITIONAL_ARGUMENT_NAMES, propertyName)) {
            return true;
        }
        if (propertyName.startsWith(JenkinsConfig.CONFIG_PREFIX)) {
            return true;
        }
        if (propertyName.startsWith(WorkflowConfig.MACRO_PREFIX)) {
            return true;
        }
        return propertyName.startsWith(ReplacementVariables.CONFIG_PREFIX);
    }

    public WorkflowFields(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
        populateConfigurableFields();
    }

    public List<ConfigurableProperty> applyConfigValues(Map<String, String> configValues, String source) {
        List<ConfigurableProperty> propertiesAffected = new ArrayList<>();
        Set<String> unknownConfigValues = new HashSet<>();
        for (String configValueName : configValues.keySet()) {
            List<WorkflowField> matchingFields = findWorkflowFieldsByConfigValue(configValueName);
            if (!matchingFields.isEmpty()) {
                propertiesAffected.add(matchingFields.get(0).configAnnotation());
                String value = configValues.get(configValueName);
                matchingFields.forEach(matchingField -> setFieldValue(matchingField, value, source));
            } else if (!isSystemProperty(configValueName)) {
                unknownConfigValues.add(configValueName);
            }
        }
        if (!unknownConfigValues.isEmpty()) {
            throw new FatalException("Unknown workflow config {} {}", pluralizeDescription(unknownConfigValues.size(), "value"), unknownConfigValues);
        }
        return propertiesAffected;
    }

    public List<WorkflowField> findWorkflowFieldsByConfigValue(String configValueName) {
        List<WorkflowField> matchingFields = new ArrayList<>();
        for (WorkflowField field : configurableFields) {
            ConfigurableProperty configurableProperty = field.configAnnotation();
            if (configurableProperty.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }
            List<String> commandLineArguments = StringUtils.splitAndTrim(configurableProperty.commandLine(), ",");

            if (commandLineArguments.contains(configValueName)) {
                matchingFields.add(field);
            }
        }
        return matchingFields;
    }

    public void overrideValues(WorkflowConfig overriddenConfig, String configFileName) {
        loadedConfigFiles.add(configFileName);
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
            } else if (existingValue != null && field.getName().equals("supportingWorkflows")) {
                List<String> existingValues = (List<String>) existingValue;
                existingValues.addAll((Collection<? extends String>) value);
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
        String configPrefixText = StringUtils.isEmpty(configPrefix) ? "" : configPrefix + ".";
        String sourceConfigProperty;
        for (WorkflowField field : configurableFields) {
            ConfigurableProperty configurableProperty = field.configAnnotation();
            String workflowConfigPropertyName = "workflow." + configPrefixText + field.getName().toLowerCase();
            String gitConfigPropertyName = configurableProperty.gitConfigProperty();
            String valueToSet;
            if (!gitConfigPropertyName.isEmpty() && StringUtils.isEmpty(configPrefix)) {
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
            if (valueToSet != null) {
                setFieldValue(field, valueToSet, "Git " + sourceConfigProperty);
            }
        }
        final String variablePrefix = "workflow." + configPrefixText + "var.";
        gitConfigValues.keySet().stream().filter(key -> key.startsWith(variablePrefix))
                .forEach(key -> workflowConfig.replacementVariables.addVariable(key.substring(variablePrefix.length()), gitConfigValues.get(key), true));

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

    public WorkflowField getFieldByName(String fieldName) {
        Optional<WorkflowField> matchingField = configurableFields.stream().filter(field -> field.getName().equals(fieldName)).findFirst();
        if (!matchingField.isPresent()) {
            throw new RuntimeException("No field found for name " + fieldName);
        }
        return matchingField.get();
    }

    public String getFieldValueSource(String fieldName) {
        String value = overriddenConfigSources.get(fieldName);
        return value != null ? value : "default";
    }

    public void markFieldAsOverridden(String fieldName, String source) {
        overriddenConfigSources.put(fieldName, source);
    }

    public Set<String> fieldSources() {
        return new HashSet<>(overriddenConfigSources.values());
    }

    public void setFieldValue(String fieldName, Object value, String source) {
        List<WorkflowField> matchingFields = configurableFields.stream()
                .filter(field -> field.getName().equals(fieldName)).collect(Collectors.toList());
        if (matchingFields.isEmpty()) {
            throw new FatalException("No configurable field found matching name " + fieldName);
        }
        matchingFields.forEach(matchingField -> setFieldValue(matchingField, value, source));
    }

    public void setFieldValue(WorkflowField field, Object value, String source) {
        Object validValue = field.determineValue(value);
        overriddenConfigSources.put(field.getName(), source);
        field.setValue(workflowConfig, validValue);
    }

    public int loadedConfigFilesSize() {
        return loadedConfigFiles.size();
    }

    public String loadedConfigFilesText() {
        return loadedConfigFiles.toString();
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

    private void populateConfigurableFields() {
        Map<String, Field> usedParams = new HashMap<>();
        addConfigurablePropertiesForClass(WorkflowConfig.class, usedParams);

        Arrays.stream(WorkflowConfig.class.getFields()).filter(field -> field.getAnnotation(SectionConfig.class) != null)
                .forEach(field -> addConfigurablePropertiesForClass(field.getType(), usedParams));
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
}
