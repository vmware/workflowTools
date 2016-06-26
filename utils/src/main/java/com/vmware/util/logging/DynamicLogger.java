package com.vmware.util.logging;

import org.slf4j.Logger;

/**
 * Log based on log level.
 */
public class DynamicLogger {

    private Logger log;

    public DynamicLogger(Logger log) {
        this.log = log;
    }

    public void log(LogLevel logLevel, String message, String... params) {
        if (logLevel == LogLevel.ERROR) {
            log.error(message, params);
        } else if (logLevel == LogLevel.INFO) {
            log.info(message, params);
        } else if (logLevel == LogLevel.DEBUG) {
            log.debug(message, params);
        } else {
            log.trace(message, params);
        }
    }
}
