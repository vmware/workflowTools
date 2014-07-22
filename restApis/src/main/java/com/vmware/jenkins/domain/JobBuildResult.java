package com.vmware.jenkins.domain;

import java.util.EnumSet;

public enum JobBuildResult {
    SUCCESS,
    FAILURE,
    ABORTED,
    BUILDING;

    public static String generateResultPattern() {
        String values = "(";
        for (Enum value : EnumSet.allOf(JobBuildResult.class)) {
            if (values.length() > 1) {
                values += "|";
            }
            values += value.name();
        }
        return values + ")";
    }

}
