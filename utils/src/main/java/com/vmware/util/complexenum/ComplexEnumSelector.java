package com.vmware.util.complexenum;

import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import static com.vmware.util.complexenum.ComplexEnum.UNKNOWN_VALUE_NAME;

/**
 * Util methods for enum classes.
 */
public class ComplexEnumSelector {

    private static final Logger log = LoggerFactory.getLogger(ComplexEnumSelector.class);

    public static ComplexEnum findByValue(Class enumType, String value) {
        Integer valueAsInt = null;
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (StringUtils.isInteger(value)) {
            valueAsInt = Integer.parseInt(value);
        }
        for (Object enumValue : EnumSet.allOf(enumType)) {
            Object valueToCompare = ((ComplexEnum)enumValue).getValue();
            if (valueToCompare instanceof Integer && valueToCompare.equals(valueAsInt)) {
                return (ComplexEnum) enumValue;
            } else if (valueToCompare instanceof String && value.equals(valueToCompare)) {
                return (ComplexEnum) enumValue;
            }
        }
        log.warn("No enum value in {} found for value {}", enumType.getSimpleName(), value);
        try {
            return (ComplexEnum) Enum.valueOf(enumType, UNKNOWN_VALUE_NAME);
        } catch (IllegalArgumentException e) {
            log.error("Enums implementing NumericalEnum must have an enum value named {}", UNKNOWN_VALUE_NAME);
            throw e;
        }
    }
}
