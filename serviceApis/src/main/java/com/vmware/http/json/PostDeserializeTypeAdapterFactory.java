package com.vmware.http.json;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

public final class PostDeserializeTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        TypeAdapter<T> originalTypeAdapter = gson.getDelegateAdapter(this, type);
        PostDeserializeTypeAdapter<T> fireTypeAdapter = new PostDeserializeTypeAdapter<T>(originalTypeAdapter, gson);
        return fireTypeAdapter;
    }
}