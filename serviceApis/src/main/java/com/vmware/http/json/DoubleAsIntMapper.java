package com.vmware.http.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class DoubleAsIntMapper implements JsonSerializer<Double> {
    @Override
    public JsonElement serialize(Double number, Type type, JsonSerializationContext jsonSerializationContext) {
        if (number != null) {
            int intValue = number.intValue();
            if (number == intValue) {
                return new JsonPrimitive(intValue);
            } else {
                return new JsonPrimitive(number);
            }
        } else {
            return null;
        }
    }
}
