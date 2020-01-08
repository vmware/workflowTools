package com.vmware.util.exception;

import com.vmware.util.StringUtils;

/**
 * Custom exception that is caught so exceptions of this type don't print out the stack trace
 */
public class FatalException extends WorkflowRuntimeException {

    public FatalException(String message, Object... arguments) {
        super(message, arguments);
    }

    public FatalException(Throwable cause, String message, Object... arguments) {
        super(cause, message, arguments);
    }
}
