package com.vmware.utils.exceptions;

/**
 * Wraps a checked IllegalAccessException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeIllegalAccessException extends RuntimeException {

    public RuntimeIllegalAccessException(IllegalAccessException cause) {
        super(cause);
    }
}

