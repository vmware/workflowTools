package com.vmware.util.exception;

public class SkipActionException extends WorkflowRuntimeException {
    public SkipActionException(String message, Object... arguments) {
        super(message, arguments);
    }
}
