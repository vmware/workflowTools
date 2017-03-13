package com.vmware.http.json;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;

import java.lang.reflect.Field;
import java.util.Map;


public class RuntimeFieldNamingStrategy implements FieldNamingStrategy {

    private FieldNamingStrategy defaultStrategy = FieldNamingPolicy.IDENTITY;

    private Map<String, String> runtimeFieldNameMappings;

    public RuntimeFieldNamingStrategy(Map<String, String> runtimeFieldNameMappings) {
        this.runtimeFieldNameMappings = runtimeFieldNameMappings;
    }

    @Override
    public String translateName(Field field) {
        RuntimeFieldName runtimeFieldName = field.getAnnotation(RuntimeFieldName.class);
        if (runtimeFieldName == null) {
            return defaultStrategy.translateName(field);
        }
        String fieldVariableName = runtimeFieldName.value();
        if (!runtimeFieldNameMappings.containsKey(fieldVariableName)) {
            throw new RuntimeException("No field name mapping for variable " + field.getName());
        }
        return runtimeFieldNameMappings.get(fieldVariableName);
    }
}
