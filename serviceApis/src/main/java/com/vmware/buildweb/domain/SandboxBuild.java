package com.vmware.buildweb.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.BuildResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents information on a sandbox build.
 */
public class SandboxBuild {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public long id;
    public String changeset;
    public String branch;
    @SerializedName("buildtype")
    public String buildType;

    @SerializedName("buildstate")
    public String buildState;

    public BuildResult getBuildResult() {
        if ("queued".equals(buildState) || "wait-for-resources".equals(buildState) || "running".equals(buildState)) {
            return BuildResult.BUILDING;
        } else if ("succeeded".equals(buildState) || "storing".equals(buildState)) {
            return BuildResult.SUCCESS;
        } else {
            log.info("Treating buildweb build state {} for build {} as a a failure", buildState, id);
            return BuildResult.FAILURE;
        }
    }
}
