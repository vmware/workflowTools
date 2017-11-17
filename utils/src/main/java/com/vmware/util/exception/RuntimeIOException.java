package com.vmware.util.exception;

import java.io.IOException;

/**
 * Wraps a checked IOException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeIOException extends WorkflowRuntimeException {

    public RuntimeIOException(IOException cause) {
        super(cause);
    }
}
