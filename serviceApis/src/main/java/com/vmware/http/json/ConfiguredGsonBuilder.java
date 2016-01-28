package com.vmware.http.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.jira.domain.IssueResolutionDefinition;
import com.vmware.jira.domain.IssueStatusDefinition;
import com.vmware.jira.domain.IssueTypeDefinition;

import java.util.Date;
import java.util.TimeZone;

/**
 * Configure a gson builder with custom type adapters and type adapters.
 */
public class ConfiguredGsonBuilder {

    private GsonBuilder builder;

    public ConfiguredGsonBuilder() {
        this(TimeZone.getDefault(), "yyyy-MM-dd HH:mm:ss");
    }

    public ConfiguredGsonBuilder(TimeZone serverTimezone, String dateFormat) {
        ImprovedExclusionStrategy serializationExclusionStrategy = new ImprovedExclusionStrategy(true);
        ImprovedExclusionStrategy deserializationExclusionStrategy = new ImprovedExclusionStrategy(false);
        this.builder = new GsonBuilder()
                .addSerializationExclusionStrategy(serializationExclusionStrategy)
                .addDeserializationExclusionStrategy(deserializationExclusionStrategy)
                .registerTypeAdapterFactory(new PostDeserializeTypeAdapterFactory())
                .registerTypeAdapter(Date.class, new DateWithTimezoneMapper(dateFormat, serverTimezone))
                .registerTypeAdapter(IssueStatusDefinition.class, new ComplexEnumMapper())
                .registerTypeAdapter(IssueResolutionDefinition.class, new ComplexEnumMapper())
                .registerTypeAdapter(IssueTypeDefinition.class, new ComplexEnumMapper());
    }

    public ConfiguredGsonBuilder setPrettyPrinting() {
        builder.setPrettyPrinting();
        return this;
    }

    public Gson build() {
        return builder.create();
    }
}
