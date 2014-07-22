package com.vmware.config;

import java.util.List;

public class UnknownWorkflowValueException extends Exception {

    public UnknownWorkflowValueException(List<String> unknownValues) {
        super("Unknown workflow values " + unknownValues.toString());
    }
}
