package com.vmware.rest.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.EnumSet;

import static com.vmware.rest.json.NumericalEnum.UNKNOWN_VALUE_NAME;

public class NumericalEnumMapper implements JsonDeserializer<NumericalEnum>, JsonSerializer<NumericalEnum> {
    private static Logger log  = LoggerFactory.getLogger(NumericalEnumMapper.class);


    @Override
    public NumericalEnum deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        int value = jsonElement.getAsInt();
        Class<Enum> enumType = (Class<Enum>) type;
        return findByValue(value, enumType);
    }

    @Override
    public JsonElement serialize(NumericalEnum numericalEnum, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(String.valueOf(numericalEnum.getCode()));
    }

    public static NumericalEnum findByValue(int value, Class<Enum> enumType) {
        for (Object enumValue : EnumSet.allOf(enumType)) {
            if (((NumericalEnum)enumValue).getCode() == value) {
                return (NumericalEnum) enumValue;
            }
        }
        log.warn("No enum value in {} found for int value {}", enumType.getSimpleName(), value);
        try {
            return (NumericalEnum) Enum.valueOf(enumType, UNKNOWN_VALUE_NAME);
        } catch (IllegalArgumentException e) {
            log.error("Enums implementing NumericalEnum must have an enum value named {}", UNKNOWN_VALUE_NAME);
            throw e;
        }
    }
}
