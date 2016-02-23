package com.vmware.http.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vmware.http.request.PostDeserializeHandler;

import java.io.IOException;

/**
 * @autor: julio
 */
public final class PostDeserializeTypeAdapter<T> extends TypeAdapter<T> {

    private final Gson gson;
    private final TypeAdapter<T> originalTypeAdapter;

    public PostDeserializeTypeAdapter(TypeAdapter<T> originalTypeAdapter, Gson gson) {
        this.gson = gson;
        this.originalTypeAdapter = originalTypeAdapter;
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        JsonElement res = originalTypeAdapter.toJsonTree(value);
        gson.toJson(res, out);
    }

    @Override
    public T read(JsonReader in) throws IOException {
        JsonElement json = new JsonParser().parse(in);

        T result = originalTypeAdapter.fromJsonTree(json);

        if (json instanceof JsonObject) {
            new PostDeserializeHandler().invokePostDeserializeMethods(result);
        }

        return result;
    }
}