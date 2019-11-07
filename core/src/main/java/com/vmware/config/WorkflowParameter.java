package com.vmware.config;

import com.vmware.util.StringUtils;

public class WorkflowParameter {

    private String name;

    private String value;

    public WorkflowParameter(String text) {
        String[] configPieces = StringUtils.splitOnlyOnce(text, "=");

        name = configPieces[0];
        // assuming that the config value is boolean if no value specified
        value = configPieces.length < 2 ? Boolean.TRUE.toString() : configPieces[1];
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
