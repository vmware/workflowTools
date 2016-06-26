package com.vmware.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

/**
 * Used to generate consistent titles.
 * Output looks like ********** TITLE **********
 */
public class Padder {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private DynamicLogger dynamicLogger = new DynamicLogger(log);
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
        this.padding = com.vmware.util.StringUtils.repeat(paddingCount, "*");
        this.title = title;
    }

    public void traceTitle() {
        logTitle(LogLevel.TRACE);
    }

    public void debugTitle() {
        logTitle(LogLevel.DEBUG);
    }

    public void infoTitle() {
        logTitle(LogLevel.INFO);
    }

    public void errorTitle() {
        logTitle(LogLevel.ERROR);
    }

    public void logTitle(LogLevel logLevel) {
        if (isFirstExecution) {
            dynamicLogger.log(logLevel, "");
        }
        dynamicLogger.log(logLevel, "{}  {}  {}", padding, title, padding);
        if (!isFirstExecution) {
            dynamicLogger.log(logLevel, "");
        }
        isFirstExecution = false;
    }

}
