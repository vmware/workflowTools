package com.vmware.utils.exceptions;

import java.net.URISyntaxException;

/**
 * Wraps a checked URISyntaxException so that it can be thrown as a Runtime Exception.
 */
public class RuntimeURISyntaxException extends RuntimeException {

    public RuntimeURISyntaxException(URISyntaxException cause) {
        super(cause);
    }
}
