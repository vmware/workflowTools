package com.vmware.util.logging;

import org.slf4j.Logger;

import java.util.logging.Level;

/**
 * Log based on log level.
 */
public class DynamicLogger {

    private Logger log;

    public DynamicLogger(Logger log) {
        this.log = log;
    }

    public void log(Level logLevel, String message, String... params) {
        if (logLevel == Level.SEVERE) {
            log.error(message, params);
        } else if (logLevel == Level.INFO) {
            log.info(message, params);
        } else if (logLevel == Level.FINE) {
            log.debug(message, params);
        } else {
            log.trace(message, params);
        }
    }
}
