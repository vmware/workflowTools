package com.vmware.reviewboard.domain;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vmware.util.StringUtils;

import java.lang.reflect.Type;
import java.util.Arrays;

public class StringArrayMapper implements JsonSerializer<String>, JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String[] values = jsonDeserializationContext.deserialize(jsonElement, String[].class);
        if (values == null) {
            return null;
        }
        return StringUtils.join(Arrays.asList(values));
    }

    @Override
    public JsonElement serialize(String s, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(s, type);
    }
}
