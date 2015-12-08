package com.vmware.rest.json;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;

/**
 * Default Gson behavior means that if @Expose is used then all properties must have it.
 * Less work to assume every property is still included unless explicitly omitted.
 */
public class ImprovedExclusionStrategy implements ExclusionStrategy {
    private boolean useForSerialization;

    public ImprovedExclusionStrategy(final boolean useForSerialization) {
        this.useForSerialization = useForSerialization;
    }

    @Override
    public boolean shouldSkipField(final FieldAttributes fieldAttributes) {
        Expose exposeAnnotation = fieldAttributes.getAnnotation(Expose.class);
        if (exposeAnnotation == null) {
            return false;
        }
        if (useForSerialization) {
            return !exposeAnnotation.serialize();
        }
        return !exposeAnnotation.deserialize();
    }

    @Override
    public boolean shouldSkipClass(final Class<?> aClass) {
        return false;
    }
}
