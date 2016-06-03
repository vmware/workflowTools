package com.vmware;

import com.vmware.util.StringUtils;

import java.util.EnumSet;

public enum BuildResult {
    SUCCESS,
    FAILURE,
    ABORTED,
    BUILDING;

    public static String generateResultPattern() {
        String patternValues = StringUtils.appendWithDelimiter("", EnumSet.allOf(BuildResult.class), "|");
        return "(" + patternValues + ")";
    }

}
