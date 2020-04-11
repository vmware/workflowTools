package com.vmware.util.logging;

import com.vmware.util.exception.FatalException;

import java.util.logging.Level;

/**
 * Using jdk logger to cut down on the size of the packaged jar.
 * Since using sl4j for the log messages, added a mapper enum to match slf4j syntax.
 */
public enum LogLevel {
    ERROR(Level.SEVERE),
    WARN(Level.WARNING),
    INFO(Level.INFO),
    DEBUG(Level.FINE),
    TRACE(Level.FINEST);
    private Level level;

    LogLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public static LogLevel fromLevel(Level level) {
        for (LogLevel logLevel : LogLevel.values()) {
            if (logLevel.getLevel().equals(level)) {
                return logLevel;
            }
        }
        throw new FatalException("No log level found for level " + level.getName());
    }

    public static LogLevel fromJschLevel(int level) {
        switch (level) {
        case 3:
        case 4:
            return ERROR;
        default:
            return DEBUG;
        }
    }
}
