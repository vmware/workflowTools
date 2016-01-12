package com.vmware.util.exception;

/**
 * Wraps a checked IllegalAccessException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeReflectiveOperationException extends RuntimeException {

    public RuntimeReflectiveOperationException(String message) {
        super(message);
    }

    public RuntimeReflectiveOperationException(ReflectiveOperationException cause) {
        super(cause);
    }
}

