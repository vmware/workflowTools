package com.vmware.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplacementVariables {
    public static final String CONFIG_PREFIX = "--V";
    private Map<String, String> replacementVariables = new HashMap<>();
    private Map<String, String> runtimeReplacementVariables = new HashMap<>();
    private WorkflowConfig config;

    public ReplacementVariables(WorkflowConfig config) {
        this.config = config;
    }

    public boolean hasVariable(VariableName variableName) {
        return replacementVariables.containsKey(variableName.name());
    }

    public void addVariable(VariableName name, String value) {
        addVariable(name.name(), value, false);
    }

    public void addVariable(String name, String valueToAdd, boolean isRuntime) {
        String value = valueToAdd;
        if (value.contains("$")) {
            for (WorkflowField configField : config.getConfigurableFields().values()) {
                if (configField.getType() != String.class || !value.contains(configField.getName())) {
                    continue;
                }
                CalculatedProperty configVariableValue = config.valueForField(configField);
                value = replaceVariable(value, configField.getName(), (String) configVariableValue.getValue());
            }
        }

        if (isRuntime) {
            Optional<String> existingKey = runtimeReplacementVariables.keySet().stream().filter(key -> key.equalsIgnoreCase(name)).findFirst();
            existingKey.ifPresent(key -> runtimeReplacementVariables.remove(key));
            runtimeReplacementVariables.put(name, value);
        } else {
            replacementVariables.put(name, value);
            updateValuesInVariables();
        }
    }

    public String replaceVariablesInValue(String existingValue) {
        String value = existingValue;
        for (String variableName : replacementVariables.keySet()) {
            value = replaceVariable(value, variableName, replacementVariables.get(variableName));
        }
        return value;
    }

    public void addReplacementVariables(Map<String, String> configValues, boolean isRuntime) {
        configValues.keySet().stream().filter(key -> key.startsWith(CONFIG_PREFIX))
                .forEach(key -> {
                    String value = configValues.get(key);
                    addVariable(key.substring(3), value, isRuntime);
                });
    }

    public boolean isEmpty() {
        return replacementVariables.isEmpty();
    }

    public Map<String, String> values() {
        return Collections.unmodifiableMap(replacementVariables);
    }

    private String replaceVariable(String value, String variableName, String variableValue) {
        if (value.contains("$" + variableName)) {
            value = value.replace("$" + variableName, variableValue);
        }
        if (value.contains("${" + variableName + "}")) {
            value = value.replace("${" + variableName + "}", variableValue);
        }
        return value;
    }

    private void updateValuesInVariables() {
        updateNamesForRuntimeVariables();

        List<String> keysSortedBySize = replacementVariables.keySet().stream().sorted((o1, o2) -> o2.length() - o1.length())
                .collect(Collectors.toList());
        for (String key : keysSortedBySize) {
            String variableValue = runtimeReplacementVariables.containsKey(key) ? runtimeReplacementVariables.get(key) : replacementVariables.get(key);
            if (!variableValue.contains("$")){
                addReplacementVariableIfDifferent(key, variableValue);
                continue;
            }
            for (Map.Entry<String, String> entry : replacementVariables.entrySet()) {
                if (entry.getKey().equals(key)) {
                    continue;
                }
                String valueToUseForReplacement = entry.getValue();
                if (runtimeReplacementVariables.containsKey(entry.getKey())) {
                    valueToUseForReplacement = runtimeReplacementVariables.get(entry.getKey());
                }
                variableValue = replaceVariable(variableValue, entry.getKey(), valueToUseForReplacement);
            }
            addReplacementVariableIfDifferent(key, variableValue);
        }
    }

    private void addReplacementVariableIfDifferent(String key, String variableValue) {
        if (!replacementVariables.get(key).equals(variableValue)) {
            replacementVariables.put(key, variableValue);
        }
    }

    private void updateNamesForRuntimeVariables() {
        Set<String> runtimeKeys = new HashSet<>(runtimeReplacementVariables.keySet());
        for (String runtimeKey : runtimeKeys) {
            String keyToReplace = keyToReplace(runtimeKey);
            if (keyToReplace != null && !keyToReplace.equals(runtimeKey)) {
                runtimeReplacementVariables.put(keyToReplace, runtimeReplacementVariables.get(runtimeKey));
                runtimeReplacementVariables.remove(runtimeKey);
            }
        }
    }

    private String keyToReplace(String keyValue){
        return replacementVariables.keySet().stream().filter(key -> key.equalsIgnoreCase(keyValue)).findFirst().orElse(null);
    }

    public enum VariableName {
        LAST_DOWNLOADED_FILE,
        REPO_DIR,
        BUILD_NUMBER
    }
}
