package com.vmware.mapping;

import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.vmware.config.WorkflowAction;
import com.vmware.config.WorkflowParameter;
import com.vmware.util.ClasspathResource;

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

    public List<WorkflowParameter> getRelevantOverriddenConfigValues(WorkflowAction action) {
        Set<String> relevantConfigValues = getConfigValuesForAction(action);
        return action.getRelevantOverriddenConfigValues(relevantConfigValues);
    }

    public Set<String> getConfigValuesForAction(WorkflowAction action) {
        Set<String> configValues = action.getConfigValues(mappings);
        // add global values
        configValues.add("--username");
        configValues.add("--dry-run");
        configValues.add("--debug");
        configValues.add("--trace");
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
