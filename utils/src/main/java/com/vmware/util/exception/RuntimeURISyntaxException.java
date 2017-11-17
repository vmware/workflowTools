package com.vmware.util.exception;

import java.net.URISyntaxException;

/**
 * Wraps a checked URISyntaxException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeURISyntaxException extends WorkflowRuntimeException {

    public RuntimeURISyntaxException(URISyntaxException cause) {
        super(cause);
    }
}
