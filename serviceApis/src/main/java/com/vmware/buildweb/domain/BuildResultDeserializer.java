package com.vmware.buildweb.domain;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.vmware.BuildStatus;

import java.lang.reflect.Type;

/**
 * Deserializes build result into enum
 */
public class BuildResultDeserializer implements JsonDeserializer<BuildStatus> {

    @Override
    public BuildStatus deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return BuildStatus.fromValue(jsonElement.getAsString());
    }
}
