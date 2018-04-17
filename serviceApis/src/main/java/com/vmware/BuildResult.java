package com.vmware;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.EnumSet;

import com.vmware.util.StringUtils;

public enum BuildResult {
    SUCCESS,
    UNSTABLE,
    FAILURE,
    ABORTED,
    BUILDING,
    STARTING;

    public static String generateResultPattern() {
        String patternValues = StringUtils.appendWithDelimiter("", EnumSet.allOf(BuildResult.class), "|");
        return "(" + patternValues + ")";
    }

    public static BuildResult fromValue(String value) {
        switch (value) {
            case "starting":
            case "building-components":
            case "queued":
            case "requesting-resources":
            case "wait-for-resources":
                return STARTING;
            case "running":
                return BUILDING;
            case "succeeded":
            case "storing":
                return SUCCESS;
            case "compile-error":
                return BuildResult.FAILURE;
            default:
                getLogger(BuildResult.class).info("Treating buildweb build state {} as a failure", value);
                return BuildResult.FAILURE;
        }
    }



}
