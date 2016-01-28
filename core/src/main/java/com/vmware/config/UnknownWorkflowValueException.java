package com.vmware.config;

import java.util.List;

public class UnknownWorkflowValueException extends RuntimeException {

    public UnknownWorkflowValueException(List<String> unknownValues) {
        super("Unknown workflow values " + unknownValues.toString());
    }
}
