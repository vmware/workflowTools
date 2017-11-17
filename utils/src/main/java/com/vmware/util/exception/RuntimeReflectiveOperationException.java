package com.vmware.util.exception;

/**
 * Wraps a checked IllegalAccessException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeReflectiveOperationException extends WorkflowRuntimeException {

    public RuntimeReflectiveOperationException(String message, String... arguments) {
        super(message, arguments);
    }

    public RuntimeReflectiveOperationException(ReflectiveOperationException cause) {
        super(cause);
    }
}

