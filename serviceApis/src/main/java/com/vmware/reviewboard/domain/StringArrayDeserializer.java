package com.vmware.reviewboard.domain;

import java.lang.reflect.Type;
import java.util.Arrays;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.vmware.util.StringUtils;

public class StringArrayDeserializer implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        String[] values = jsonDeserializationContext.deserialize(jsonElement, String[].class);
        if (values == null) {
            return null;
        }
        return StringUtils.join(Arrays.asList(values));
    }
}
