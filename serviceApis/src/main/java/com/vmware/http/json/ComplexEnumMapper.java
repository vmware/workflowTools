package com.vmware.http.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vmware.ComplexEnum;
import com.vmware.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.EnumSet;

import static com.vmware.ComplexEnum.UNKNOWN_VALUE_NAME;

public class ComplexEnumMapper implements JsonDeserializer<ComplexEnum>, JsonSerializer<ComplexEnum> {
    private static Logger log  = LoggerFactory.getLogger(ComplexEnumMapper.class);


    @Override
    public ComplexEnum deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String value = jsonElement.getAsString();
        Class<Enum> enumType = (Class<Enum>) type;
        return findByValue(enumType, value);
    }

    @Override
    public JsonElement serialize(ComplexEnum complexEnum, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(String.valueOf(complexEnum.getValue()));
    }

    public static ComplexEnum findByValue(Class enumType, String value) {
        Integer valueAsInt = null;
        if (StringUtils.isBlank(value)) {
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
