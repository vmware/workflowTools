package com.vmware.jira.domain;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class IssueTypesDefinitionMapper implements JsonSerializer<IssueTypeDefinition[]>, JsonDeserializer<IssueTypeDefinition[]> {

    @Override
    public IssueTypeDefinition[] deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Integer[] issueTypeValues = jsonDeserializationContext.deserialize(jsonElement, Integer[].class);
        if (issueTypeValues == null) {
            return null;
        }
        List<IssueTypeDefinition> issueTypeValuesList = new ArrayList<>();
        for (int issueTypeValue : issueTypeValues) {
            issueTypeValuesList.add(IssueTypeDefinition.fromValue(issueTypeValue));
        }
        return issueTypeValuesList.toArray(new IssueTypeDefinition[issueTypeValuesList.size()]);
    }

    @Override
    public JsonElement serialize(IssueTypeDefinition[] issueTypeDefinitions, Type type, JsonSerializationContext jsonSerializationContext) {
        if (issueTypeDefinitions == null) {
            return jsonSerializationContext.serialize(null);
        }
        List<Integer> issueTypeIntValues = new ArrayList<>();
        for (IssueTypeDefinition issueTypeDefinition : issueTypeDefinitions) {
            issueTypeIntValues.add(issueTypeDefinition.getValue());
        }
        return jsonSerializationContext.serialize(issueTypeIntValues.toArray(new Integer[issueTypeIntValues.size()]), Integer[].class);
    }
}
