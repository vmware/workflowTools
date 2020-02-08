package com.vmware.reviewboard.domain;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;

public class RequestLinksArrayDeserializer implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Link[] links = jsonDeserializationContext.deserialize(jsonElement, Link[].class);
        if (links == null) {
            return null;
        }
        return Arrays.stream(links).map(this::parseRequestId).collect(Collectors.joining(","));
    }

    private String parseRequestId(Link link) {
        return MatcherUtils.singleMatch(link.getHref(), "review-requests/(\\d+)");
    }
}
