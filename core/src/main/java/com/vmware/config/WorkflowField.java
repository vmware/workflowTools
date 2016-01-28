package com.vmware.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Used for determining the valid value for a workflow config field.
 */
public class WorkflowField {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Field field;

    public WorkflowField(Field field) {
        this.field = field;
    }

    public Object determineValue(String sourceValue) {
        if (sourceValue == null) {
            return null;
        }
        Object validValue = null;
        Class fieldType = field.getType();
        if (fieldType == String[].class) {
            validValue = sourceValue.split(",");
        } else if (fieldType == int[].class) {
            String[] values = sourceValue.split(",");
            int[] intValues = new int[values.length];
            for (int i = 0; i < values.length; i ++) {
                intValues[i] = Integer.parseInt(values[i].trim());
            }
            validValue = intValues;
        } else if (fieldType == int.class) {
            validValue = Integer.parseInt(sourceValue);
        } else if (fieldType == boolean.class) {
            validValue = Boolean.valueOf(sourceValue);
        } else if (fieldType == String[].class) {
            validValue = sourceValue.split(",");
        } else if (fieldType == SortedSet.class) {
            validValue = new TreeSet<String>();
            ((SortedSet) validValue).addAll(Arrays.asList(sourceValue.split(",")));
        } else if (fieldType == String.class) {
            validValue = sourceValue;
        } else {
            log.error("Cannot set configuration property {} of type {} from git config value",
                    field.getName(), fieldType.getSimpleName());
        }
        return validValue;
    }
}
