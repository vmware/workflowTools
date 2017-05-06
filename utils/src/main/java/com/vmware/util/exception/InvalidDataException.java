package com.vmware.util.exception;

import com.vmware.util.StringUtils;

/**
 * Custom exception that is caught so exceptions of this type don't print out the stack trace
 */
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String message, String... arguments) {
        super(StringUtils.addArgumentsToValue(message, arguments));
    }
}
