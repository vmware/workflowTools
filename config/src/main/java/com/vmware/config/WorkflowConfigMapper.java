package com.vmware.config;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.vmware.config.section.SectionConfig;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.exception.RuntimeReflectiveOperationException;

public class WorkflowConfigMapper implements JsonDeserializer<WorkflowConfig>, JsonSerializer<WorkflowConfig> {
    @Override
    public WorkflowConfig deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Class typeClass = (Class) type;
        List<Field> additionalConfigFields = Arrays.stream(typeClass.getFields())
                .filter(field -> field.getAnnotation(SectionConfig.class) != null).collect(Collectors.toList());

        WorkflowConfig config = (WorkflowConfig) ReflectionUtils.newInstance(typeClass);

        List<Field> configurableFields = Arrays.stream(typeClass.getFields())
                .filter(field -> field.getAnnotation(ConfigurableProperty.class) != null).collect(Collectors.toList());

        JsonObject configJsonObject = jsonElement.getAsJsonObject();
        configurableFields.forEach(field -> {
            String fieldName = determineNameToUseForField(field);
            JsonElement fieldJsonObject = configJsonObject.get(fieldName);
            if (fieldJsonObject != null) {
                Object fieldValue;
                if (field.getAnnotation(JsonAdapter.class) != null) {
                    JsonDeserializer customDeserializer = (JsonDeserializer) ReflectionUtils.newInstance(field.getAnnotation(JsonAdapter.class).value());
                    fieldValue = customDeserializer.deserialize(fieldJsonObject, field.getType(), jsonDeserializationContext);
                } else {
                    fieldValue = jsonDeserializationContext.deserialize(fieldJsonObject, field.getType());
                }
                setFieldValue(field, config, fieldValue);
            }
        });

        additionalConfigFields.forEach(field -> {
            Object fieldConfig = jsonDeserializationContext.deserialize(jsonElement, field.getType());
            setFieldValue(field, config, fieldConfig);
        });
        return config;
    }

    @Override
    public JsonElement serialize(WorkflowConfig workflowConfig, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject configJsonObject = new JsonObject();

        Class typeClass = (Class) type;
        List<Field> additionalConfigFields = Arrays.stream(typeClass.getFields())
                .filter(field -> field.getAnnotation(SectionConfig.class) != null).collect(Collectors.toList());

        serializeConfigurableProperties(configJsonObject, workflowConfig, jsonSerializationContext);

        additionalConfigFields.forEach(field -> {
            Object fieldValue = ReflectionUtils.getValue(field, workflowConfig);
            if (fieldValue != null) {
                serializeConfigurableProperties(configJsonObject, fieldValue, jsonSerializationContext);
            }
        });

        return configJsonObject;
    }

    private void serializeConfigurableProperties(JsonObject configJsonObject, Object configInstance, JsonSerializationContext jsonSerializationContext) {
        List<Field> configurableFields = Arrays.stream(configInstance.getClass().getFields())
                .filter(field -> field.getAnnotation(ConfigurableProperty.class) != null).collect(Collectors.toList());

        configurableFields.forEach(field -> {
            String fieldName = determineNameToUseForField(field);
            Object fieldValue = ReflectionUtils.getValue(field, configInstance);
            if (fieldValue != null) {
                JsonElement fieldJsonObject;
                if (field.getAnnotation(JsonAdapter.class) != null) {
                    try {
                        JsonSerializer customSerializer = (JsonSerializer) field.getAnnotation(JsonAdapter.class).value().newInstance();
                        fieldJsonObject = customSerializer.serialize(fieldValue, field.getType(), jsonSerializationContext);
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeReflectiveOperationException(e);
                    }
                } else {
                    fieldJsonObject = jsonSerializationContext.serialize(fieldValue);
                }
                configJsonObject.add(fieldName, fieldJsonObject);
            }
        });
    }

    private void setFieldValue(Field field, WorkflowConfig config, Object value) {
        try {
            field.set(config, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String determineNameToUseForField(Field field) {
        SerializedName serializedNameAnnotation = field.getAnnotation(SerializedName.class);
        return serializedNameAnnotation != null ? serializedNameAnnotation.value() : field.getName();
    }
}
