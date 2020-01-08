package com.vmware.util.exception;

import com.vmware.util.StringUtils;

public class WorkflowRuntimeException extends RuntimeException {

    protected WorkflowRuntimeException(String message, Object... arguments) {
        super(StringUtils.addArgumentsToValue(message, arguments));
    }

    protected WorkflowRuntimeException(Throwable cause, String message, Object... arguments) {
        super(StringUtils.addArgumentsToValue(message, arguments), cause);
    }

    protected WorkflowRuntimeException(Throwable cause) {
        super(cause);
    }
}
