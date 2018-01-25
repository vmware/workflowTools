package com.vmware.buildweb.domain;

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

    @SerializedName("buildstate")
    public String buildState;

    @SerializedName("_buildtree_url")
    public String buildTreeUrl;

    @SerializedName("_buildmachines_url")
    public String buildMachinesUrl;

    public BuildResult buildResult() {
        return BuildResult.fromValue(buildState);
    }

    public String relativeBuildTreePath() {
        URI buildUri = URI.create(buildTreeUrl);
        return buildUri.getPath();
    }
}
