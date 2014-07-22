package com.vmware.mapping;

import com.vmware.action.AbstractAction;
import com.vmware.utils.ClasspathResource;

import com.google.gson.Gson;

import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the mapping of config values to a workflow action.
 * This is used for command line tab completion of workflow config values.
 */
public class ConfigMappings {

    private Map<String, List<String>> mappings;

    public ConfigMappings() {
        Reader reader = new ClasspathResource("/configValueMappings.json").getReader();
        this.mappings = new Gson().fromJson(reader, Map.class);
    }


    public List<String> get(String keyName) {
        return mappings.get(keyName);
    }

    public Set<String> keySet() {
        return mappings.keySet();
    }

    public Set<String> getConfigValuesForAction(Class<? extends AbstractAction> foundAction) {
        Set<String> configValues = new HashSet<String>();
        Class classToGetValuesFor = foundAction;
        while (classToGetValuesFor != Object.class) {
            List<String> configValuesForClass = mappings.get(classToGetValuesFor.getSimpleName());
            if (configValuesForClass != null) {
                configValues.addAll(configValuesForClass);
            }
            classToGetValuesFor = classToGetValuesFor.getSuperclass();
        }
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
