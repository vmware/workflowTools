package com.vmware.http.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vmware.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.EnumSet;

import static com.vmware.http.json.StringEnum.UNKNOWN_VALUE_NAME;

public class StringEnumMapper implements JsonDeserializer<StringEnum>, JsonSerializer<StringEnum> {
    private static Logger log  = LoggerFactory.getLogger(StringEnumMapper.class);


    @Override
    public StringEnum deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String value = jsonElement.getAsString();
        Class<Enum> enumType = (Class<Enum>) type;
        return findByValue(enumType, value);
    }

    @Override
    public JsonElement serialize(StringEnum enumValue, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(enumValue.getValue());
    }

    public static StringEnum findByValue(Class enumClass, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        Class<Enum> enumType = (Class<Enum>) enumClass;
        for (Object enumValue : EnumSet.allOf(enumType)) {
            if (((StringEnum) enumValue).getValue().equals(value)) {
                return (StringEnum) enumValue;
            }
        }
        log.warn("No enum value in {} found for value {}", enumType.getSimpleName(), value);
        try {
            return (StringEnum) Enum.valueOf(enumType, UNKNOWN_VALUE_NAME);
        } catch (IllegalArgumentException e) {
            log.error("Enums implementing NumericalEnum must have an enum value named {}", UNKNOWN_VALUE_NAME);
            throw e;
        }
    }
}