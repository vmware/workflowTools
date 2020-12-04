package com.vmware;

import com.vmware.util.StringUtils;

import java.util.EnumSet;

import static org.slf4j.LoggerFactory.getLogger;

public enum BuildStatus {
    SUCCESS,
    UNSTABLE,
    FAILURE,
    ABORTED,
    BUILDING,
    STARTING;

    public static String generateResultPattern() {
        String patternValues = StringUtils.appendWithDelimiter("", EnumSet.allOf(BuildStatus.class), "|");
        return "(" + patternValues + ")";
    }

    public static BuildStatus fromValue(String value) {
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
                return BuildStatus.FAILURE;
            default:
                getLogger(BuildStatus.class).info("Treating buildweb build state {} as a failure", value);
                return BuildStatus.FAILURE;
        }
    }



}
