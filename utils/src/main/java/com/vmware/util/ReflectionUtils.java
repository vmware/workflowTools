package com.vmware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vmware.util.exception.RuntimeReflectiveOperationException;

/**
 * Util methods for invoking reflection methods and wrapping the exceptions with runtime exceptions.
 */
public class ReflectionUtils {

    public static List<Class> collectClassHierarchyInDescendingOrder(Class clazz) {
        List<Class> classes = new ArrayList<>();
        do {
            classes.add(0, clazz);
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class);
        return classes;
    }

    public static Class forName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    public static Object newInstance(Class clazz, Object... constructorParameters) {
        try {
            List<Class> parameterClasses =
                    Arrays.stream(constructorParameters).map(Object::getClass).collect(Collectors.toList());
            Class[] parameterClassesArray = parameterClasses.toArray(new Class[parameterClasses.size()]);
            return clazz.getConstructor(parameterClassesArray).newInstance(constructorParameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    public static void invokeMethod(Method method, Object instance, Object... args) {
        try {
            method.invoke(instance, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    public static void invokeAllMethodsWithAnnotation(Object instance, Class<? extends Annotation> annotation) {
        List<Method> annotatedMethods = new ArrayList<>();
        Class clazzToFetch = instance.getClass();
        while (clazzToFetch != Object.class) {
            List<Method> matchingMethods = Arrays.stream(clazzToFetch.getMethods())
                    .filter(method -> method.getAnnotation(annotation) != null).collect(Collectors.toList());
            annotatedMethods.addAll(matchingMethods);
            clazzToFetch = clazzToFetch.getSuperclass();
        }
        annotatedMethods.forEach(method -> {
            try {
                method.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Field getField(Class clazz, String fieldName) {
        try {
            return clazz.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    public static List<Field> getAllFieldsWithoutAnnotation(Class clazz, Class<? extends Annotation> ignoreAnnotation) {
        Class clazzToFetch = clazz;
        List<Field> fieldsToReturn = new ArrayList<>();
        while (clazzToFetch != Object.class) {
            List<Field> matchingFields = Arrays.stream(clazzToFetch.getFields())
                    .filter(field -> ignoreAnnotation == null || field.getAnnotation(ignoreAnnotation) == null).collect(Collectors.toList());
            fieldsToReturn.addAll(matchingFields);
            clazzToFetch = clazzToFetch.getSuperclass();
        }
        return fieldsToReturn;
    }


    public static Object getValue(Field field, Object instance) {
        try {
            Object instanceToUse = determineInstanceForField(field, instance);
            return field.get(instanceToUse);
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    public static void setValue(Field field, Object instance, Object value) {
        try {
            Object instanceToUse = determineInstanceForField(field, instance);
            if (field.getType().isArray() && value != null && value.getClass() == Object[].class && field.getType() != Object[].class) {

                Object[] objectValues = (Object[]) value;
                Object typedArray = Array.newInstance(field.getType().getComponentType(), objectValues.length);
                System.arraycopy(objectValues, 0, typedArray, 0, objectValues.length);
                field.set(instance, typedArray);
            } else if (field.getType().isArray() && value != null && java.sql.Array.class.isAssignableFrom(value.getClass())) {
                Object[] objectValues = (Object[]) ((java.sql.Array) value).getArray();
                Object typedArray = Array.newInstance(field.getType().getComponentType(), objectValues.length);
                System.arraycopy(objectValues, 0, typedArray, 0, objectValues.length);
                field.set(instance, typedArray);
            } else {
                field.set(instanceToUse, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object determineInstanceForField(Field field, Object instance) throws IllegalAccessException {
        if (field.getDeclaringClass().isAssignableFrom(instance.getClass())) {
            return instance;
        }
        Optional<Field> matchingAdditionalConfigField = Arrays.stream(instance.getClass().getFields())
                .filter(configField -> configField.getType() == field.getDeclaringClass()).findFirst();
        if (!matchingAdditionalConfigField.isPresent()) {
            throw new RuntimeException("Failed to find field for class " + field.getDeclaringClass().getName() + " in " + instance.getClass().getName());
        }
        return matchingAdditionalConfigField.get().get(instance);
    }
}
