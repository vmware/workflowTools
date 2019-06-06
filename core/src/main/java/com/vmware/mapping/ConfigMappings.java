package com.vmware.mapping;

import com.vmware.action.BaseAction;
import com.vmware.action.git.GenerateGitCommitStats;
import com.vmware.config.ActionDescription;
import com.vmware.util.ClasspathResource;

import com.google.gson.Gson;

import java.io.Reader;
import java.lang.annotation.Annotation;
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
        Reader reader = new ClasspathResource("/configValueMappings.json", this.getClass()).getReader();
        this.mappings = new Gson().fromJson(reader, Map.class);
    }


    public List<String> get(String keyName) {
        return mappings.get(keyName);
    }

    public Set<String> keySet() {
        return mappings.keySet();
    }

    public Set<String> getConfigValuesForAction(Class<? extends BaseAction> foundAction) {
        Set<String> configValues = new HashSet<String>();
        Class classToGetValuesFor = foundAction;
        while (classToGetValuesFor != Object.class) {
            List<String> configValuesForClass = mappings.get(classToGetValuesFor.getSimpleName());
            if (configValuesForClass != null) {
                configValues.addAll(configValuesForClass);
            }
            boolean ignoreSuperClass = false;
            if (classToGetValuesFor != BaseAction.class && classToGetValuesFor.isAnnotationPresent(ActionDescription.class)) {
                Class<? extends BaseAction> actionClass = classToGetValuesFor;
                ignoreSuperClass = actionClass.getAnnotation(ActionDescription.class).ignoreConfigValuesInSuperclass();
            }
            classToGetValuesFor = ignoreSuperClass ? Object.class : classToGetValuesFor.getSuperclass();
        }
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

    public static void main(String[] args) {
        ConfigMappings configMappings = new ConfigMappings();
        System.out.println(configMappings.getConfigValuesForAction(GenerateGitCommitStats.class));
    }
}
