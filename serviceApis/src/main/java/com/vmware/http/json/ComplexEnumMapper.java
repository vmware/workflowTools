package com.vmware.http.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vmware.utils.enums.ComplexEnum;
import com.vmware.utils.StringUtils;
import com.vmware.utils.enums.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.EnumSet;

import static com.vmware.utils.enums.ComplexEnum.UNKNOWN_VALUE_NAME;

public class ComplexEnumMapper implements JsonDeserializer<ComplexEnum>, JsonSerializer<ComplexEnum> {
    private static Logger log  = LoggerFactory.getLogger(ComplexEnumMapper.class);


    @Override
    public ComplexEnum deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String value = jsonElement.getAsString();
        Class<Enum> enumType = (Class<Enum>) type;
        return EnumUtils.findByValue(enumType, value);
    }

    @Override
    public JsonElement serialize(ComplexEnum complexEnum, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(String.valueOf(complexEnum.getValue()));
    }

}
