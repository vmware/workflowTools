package com.vmware.config;

import com.vmware.util.StringUtils;

public class WorkflowParameter {

    private final String name;

    private final String value;

    public WorkflowParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

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
