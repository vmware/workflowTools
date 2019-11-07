package com.vmware.config;

public class CalculatedProperty {
    private Object value;

    private String source;

    public CalculatedProperty(Object value, String source) {
        this.value = value;
        this.source = source;
    }

    public Object getValue() {
        return value;
    }

    public String getSource() {
        return source;
    }
}
