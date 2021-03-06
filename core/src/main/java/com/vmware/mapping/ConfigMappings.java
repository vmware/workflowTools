package com.vmware.mapping;

import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.config.WorkflowAction;
import com.vmware.util.ClasspathResource;
import com.vmware.util.CollectionUtils;

/**
 * Represents the mapping of config values to a workflow action.
 * This is used for command line tab completion of workflow config values.
 */
public class ConfigMappings {

    private Map<String, List<String>> mappings;

    public ConfigMappings() {
        Reader reader = new ClasspathResource("/configValueMappings.json", this.getClass()).getReader();
        this.mappings = new Gson().fromJson(reader, Map.class);
    }


    public List<String> get(String keyName) {
        return mappings.get(keyName);
    }

    public Set<String> keySet() {
        return mappings.keySet();
    }

    public Set<String> getAutoCompleteValuesForAction(WorkflowAction action) {
        Set<String> configValues = getConfigValuesForAction(action, true);
        Set<String> workflowActionParameters = action.getWorkflowParameterNames();
        if (CollectionUtils.isNotEmpty(action.getOverriddenConfigValues())) {
            configValues.removeIf(workflowActionParameters::contains);
        }
        configValues.remove("--file-data");
        return configValues;
    }

    public Set<String> getConfigValuesForAction(WorkflowAction action, boolean autoCompleteValuesOnly) {
        Set<String> configValues = action.getConfigValues(mappings, autoCompleteValuesOnly);
        // add global values
        configValues.add("--dry-run");
        configValues.add("--debug");
        return configValues;
    }

    public Set<String> allConfigValues() {
        Set<String> configValues = new HashSet<String>();
        for (String className : mappings.keySet()) {
            configValues.addAll(mappings.get(className));
        }
        return configValues;
    }

}
