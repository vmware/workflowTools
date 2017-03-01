package com.vmware.reviewboard.domain;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class LinkArrayMapper implements JsonSerializer<String>, JsonDeserializer<String> {

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

    @Override
    public JsonElement serialize(String s, Type type, JsonSerializationContext jsonSerializationContext) {
        return jsonSerializationContext.serialize(s, type);
    }
}
