package com.vmware.utils.exceptions;

/**
 * Wraps a checked IllegalAccessException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeReflectiveOperationException extends RuntimeException {

    public RuntimeReflectiveOperationException(ReflectiveOperationException cause) {
        super(cause);
    }
}

