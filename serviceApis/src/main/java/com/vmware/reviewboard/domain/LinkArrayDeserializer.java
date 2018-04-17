package com.vmware.reviewboard.domain;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class LinkArrayDeserializer implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Link[] links = jsonDeserializationContext.deserialize(jsonElement, Link[].class);
        if (links == null) {
            return null;
        }
        String groupNames = "";
        for (Link link : links) {
            if (!groupNames.isEmpty()) {
                groupNames += ",";
            }
            groupNames += link.getTitle();
        }
        return groupNames;
    }
}
