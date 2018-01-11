package com.vmware.buildweb.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.BuildResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Represents information on a buildweb build.
 * Can either be an official build or a sandbox build
 */
public class BuildwebBuild {

    private Logger log = LoggerFactory.getLogger(this.getClass());

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

    public BuildResult getBuildResult() {
        if ("queued".equals(buildState) || "requesting-resources".equals(buildState)
                || "wait-for-resources".equals(buildState) || "running".equals(buildState)) {
            return BuildResult.BUILDING;
        } else if ("succeeded".equals(buildState) || "storing".equals(buildState)) {
            return BuildResult.SUCCESS;
        } else {
            log.info("Treating buildweb build state {} for build {} as a a failure", buildState, id);
            return BuildResult.FAILURE;
        }
    }

    public String relativeBuildTreePath() {
        URI buildUri = URI.create(buildTreeUrl);
        return buildUri.getPath();
    }
}
