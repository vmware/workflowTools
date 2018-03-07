package com.vmware.buildweb.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildResult;

import java.net.URI;

/**
 * Represents information on a buildweb build.
 * Can either be an official build or a sandbox build
 */
public class BuildwebBuild {

    public long id;
    public String changeset;
    public String branch;
    @SerializedName("buildtype")
    public String buildType;

    @Expose(serialize = false)
    @SerializedName("buildstate")
    @JsonAdapter(BuildResultDeserializer.class)
    public BuildResult buildResult;

    @SerializedName("_buildtree_url")
    public String buildTreeUrl;

    @SerializedName("_buildmachines_url")
    public String buildMachinesUrl;

    public String relativeBuildTreePath() {
        URI buildUri = URI.create(buildTreeUrl);
        return buildUri.getPath();
    }
}
