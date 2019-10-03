package com.vmware.config;

public class CalculatedProperty {
    private String value;

    private String source;

    public CalculatedProperty(String value, String source) {
        this.value = value;
        this.source = source;
    }

    public String getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }
}
