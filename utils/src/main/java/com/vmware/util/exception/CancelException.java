package com.vmware.util.exception;

import com.vmware.util.logging.LogLevel;

public class CancelException extends WorkflowRuntimeException {

    private final LogLevel logLevel;

    public CancelException(LogLevel logLevel, String message, String... arguments) {
        super(message, arguments);
        this.logLevel = logLevel;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }
}
