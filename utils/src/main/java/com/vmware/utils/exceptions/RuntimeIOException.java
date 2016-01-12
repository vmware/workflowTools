package com.vmware.utils.exceptions;

import java.io.IOException;

/**
 * Wraps a checked IOException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeIOException extends RuntimeException {

    public RuntimeIOException(IOException cause) {
        super(cause);
    }
}
