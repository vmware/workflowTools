package com.vmware.jenkins.domain;

import com.vmware.utils.StringUtils;

import java.util.EnumSet;

public enum JobBuildResult {
    SUCCESS,
    FAILURE,
    ABORTED,
    BUILDING;

    public static String generateResultPattern() {
        String patternValues = StringUtils.appendWithDelimiter("", EnumSet.allOf(JobBuildResult.class), "|");
        return "(" + patternValues + ")";
    }

}
