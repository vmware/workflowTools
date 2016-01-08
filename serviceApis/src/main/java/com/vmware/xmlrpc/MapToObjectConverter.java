package com.vmware.xmlrpc;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.utils.enums.ComplexEnum;
import com.vmware.http.json.ComplexEnumMapper;
import com.vmware.http.request.DeserializedName;
import com.vmware.http.request.PostDeserialization;
import com.vmware.utils.enums.EnumUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Constructs an object from a map of values.
 */
public class MapToObjectConverter {

    public <T> T convert(Map values, Class<T> objectClass) {
        Object createdObject;
        try {
            createdObject = objectClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

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

            try {
                setFieldValue(createdObject, field, valueToConvert);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        for (Method method : objectClass.getMethods()) {
            if (method.getAnnotation(PostDeserialization.class) == null) {
                continue;
            }

            try {
                method.invoke(createdObject);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return (T) createdObject;
    }

    private void setFieldValue(Object createdObject, Field field, Object valueToConvert) throws IllegalAccessException {
        Class fieldType = field.getType();
        field.setAccessible(true);

        if (valueToConvert.getClass() == fieldType) {
            field.set(createdObject, valueToConvert);
        } else if (fieldType == String.class) {
            field.set(createdObject, String.valueOf(valueToConvert));
        } else if (ComplexEnum.class.isAssignableFrom(fieldType)) {
            field.set(createdObject, EnumUtils.findByValue(fieldType, String.valueOf(valueToConvert)));
        } else if (fieldType.isArray() && valueToConvert instanceof Object[]) {
            Class arrayObjectType = fieldType.getComponentType();
            Object[] valuesToConvert = (Object[]) valueToConvert;
            Object convertedValues = Array.newInstance(arrayObjectType, valuesToConvert.length);
            for (int i = 0; i < valuesToConvert.length; i++) {
                Map listObjectValues = (Map) valuesToConvert[i];
                Array.set(convertedValues, i, convert(listObjectValues, arrayObjectType));
            }
            field.set(createdObject, convertedValues);
        } else {
            field.setAccessible(false);
            throw new RuntimeException("Cannot set value of type " + valueToConvert.getClass().getSimpleName()
                    + " for field of type " + fieldType.getSimpleName());
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
}
