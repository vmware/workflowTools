package com.vmware.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

/**
 * Used to generate consistent titles.
 * Output looks like ********** TITLE **********
 */
public class Padder {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private static final int PADDING_LENGTH = 80;

    private final String title;
    private String padding;
    private boolean isFirstExecution = true;

    public Padder(String title, Object... args) {
        for (Object arg : args) {
            title = title.replaceFirst("\\{}", String.valueOf(arg));
        }
        // add a trailing space if the length is not even
        if (title.length() % 2 != 0) {
            title += " ";
        }
        int paddingCount = (PADDING_LENGTH - (title.length() + 4)) / 2;
        this.padding = StringUtils.repeat(paddingCount, "*");
        this.title = title.toUpperCase();
    }

    public void traceTitle() {
        logTitle(Level.FINEST);
    }

    public void debugTitle() {
        logTitle(Level.FINE);
    }

    public void infoTitle() {
        logTitle(Level.INFO);
    }

    public void errorTitle() {
        logTitle(Level.SEVERE);
    }

    public void logTitle(Level logLevel) {
        if (isFirstExecution) {
            log(logLevel, "");
        }
        log(logLevel, "{}  {}  {}", padding, title, padding);
        if (!isFirstExecution) {
            log(logLevel, "");
        }
        isFirstExecution = false;
    }

    private void log(Level logLevel, String message, String... params) {
        if (logLevel == Level.SEVERE) {
            log.error(message, params);
        } else if (logLevel == Level.INFO) {
            log.info(message, params);
        } else if (logLevel == Level.FINE) {
            log.debug(message, params);
        } else if (logLevel == Level.FINER || logLevel == Level.FINEST) {
            log.trace(message, params);
        }
    }
}
