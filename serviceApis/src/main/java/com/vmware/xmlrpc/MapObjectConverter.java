package com.vmware.xmlrpc;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.http.request.DeserializedName;
import com.vmware.http.request.PostDeserializeHandler;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.complexenum.ComplexEnum;
import com.vmware.util.complexenum.ComplexEnumSelector;
import com.vmware.util.exception.RuntimeReflectiveOperationException;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Constructs an object to and from a map of values.
 */
public class MapObjectConverter {

    public Map<String, Object> toMap(Object requestObject) {
        Map<String, Object> valuesToWrite = new HashMap<>();
        for (Field field : requestObject.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (Modifier.isPrivate(field.getModifiers())) {
                continue;
            }
            Expose expose = field.getAnnotation(Expose.class);
            if (expose != null && !expose.serialize()) {
                continue;
            }
            Object value = ReflectionUtils.getValue(field, requestObject);
            if (value == null) {
                continue;
            }
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            String nameToUse = serializedName != null ? serializedName.value() : field.getName();
            Object valueToUse = determineCorrectValue(value);
            if (valueToUse != null) {
                valuesToWrite.put(nameToUse, valueToUse);
            }
        }
        return valuesToWrite;
    }

    public <T> T fromMap(Map values, Class<T> objectClass) {
        Object createdObject = ReflectionUtils.newInstance(objectClass);
        for (Field field : objectClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            Expose exposeAnnotation = field.getAnnotation(Expose.class);
            if (exposeAnnotation != null && !exposeAnnotation.deserialize()) {
                continue;
            }

            String nameToUse = determineNameToUseForField(field);

            if (!values.containsKey(nameToUse)) {
                continue;
            }

            Object valueToConvert = values.get(nameToUse);

            setFieldValue(createdObject, field, valueToConvert);
        }

        new PostDeserializeHandler().invokePostDeserializeMethods(createdObject);

        return (T) createdObject;
    }

    private Object determineCorrectValue(Object objectToCheck) {
        if (objectToCheck instanceof byte[]) {
            return objectToCheck;
        } else if (objectToCheck instanceof Boolean) {
            Boolean bool = (Boolean) objectToCheck;
            return bool ? "1" : "0";
        }
        return objectToCheck.toString();
    }

    private void setFieldValue(Object createdObject, Field field, Object valueToConvert) {
        Class fieldType = field.getType();
        field.setAccessible(true);

        if (valueToConvert.getClass() == fieldType) {
            ReflectionUtils.setValue(field, createdObject, valueToConvert);
        } else if (fieldType == String.class) {
            ReflectionUtils.setValue(field, createdObject, convertObjectToString(valueToConvert));
        } else if (ComplexEnum.class.isAssignableFrom(fieldType)) {
            ReflectionUtils.setValue(field, createdObject, ComplexEnumSelector.findByValue(fieldType, String.valueOf(valueToConvert)));
        } else if (fieldType.isArray() && valueToConvert instanceof Object[]) {
            Class arrayObjectType = fieldType.getComponentType();
            Object[] valuesToConvert = (Object[]) valueToConvert;
            Object convertedValues = Array.newInstance(arrayObjectType, valuesToConvert.length);
            for (int i = 0; i < valuesToConvert.length; i++) {
                Map listObjectValues = (Map) valuesToConvert[i];
                Array.set(convertedValues, i, fromMap(listObjectValues, arrayObjectType));
            }
            ReflectionUtils.setValue(field, createdObject, convertedValues);
        } else if (valueToConvert instanceof Map) {
            ReflectionUtils.setValue(field, createdObject, fromMap((Map) valueToConvert, fieldType));
        } else {
            field.setAccessible(false);
            throw new RuntimeReflectiveOperationException(format("Cannot set value of type %s for field of type %s",
                    valueToConvert.getClass().getSimpleName(), fieldType.getSimpleName()));
        }
        field.setAccessible(false);
    }

    private String determineNameToUseForField(Field field) {
        String nameToUse = field.getName();
        DeserializedName deSerializedNameAnnotation = field.getAnnotation(DeserializedName.class);
        SerializedName serializedNameAnnotation = field.getAnnotation(SerializedName.class);

        if (deSerializedNameAnnotation != null) {
            nameToUse = deSerializedNameAnnotation.value();
        } else if (serializedNameAnnotation != null) {
            nameToUse = serializedNameAnnotation.value();
        }
        return nameToUse;
    }

    private String convertObjectToString(Object valueToConvert) {
        if (valueToConvert instanceof String) {
            return (String) valueToConvert;
        } else if (valueToConvert instanceof byte[]) {
            return IOUtils.read(new ByteArrayInputStream((byte[]) valueToConvert));
        } else if (valueToConvert != null) {
            return String.valueOf(valueToConvert);
        } else {
            return null;
        }
    }
}
