package com.vmware.util.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.util.StringUtils;

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
        title = title.trim();
        for (Object arg : args) {
            title = title.replaceFirst("\\{}", String.valueOf(arg));
        }
        int fullTitleLength = title.length() + 4;
        int paddingCount = (PADDING_LENGTH - fullTitleLength) / 2;
        if (fullTitleLength > PADDING_LENGTH) {
            title = title.substring(0, PADDING_LENGTH - 7) + "...";
            paddingCount = 1;
        }
        this.padding = StringUtils.repeat(paddingCount, "*");
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

    public void warnTitle() {
        logTitle(LogLevel.WARN);
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

    public String getTitle() {
        return title;
    }
}
