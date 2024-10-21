package com.vmware.util.logging;

import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to generate consistent titles.
 * Output looks like ********** TITLE **********
 */
public class Padder {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final DynamicLogger dynamicLogger = new DynamicLogger(log);
    private static final int DEFAULT_PADDING_LENGTH = 80;
    private static final int TITLE_SPACING_LENGTH = 4;
    private static final int MIN_PADDING_LENGTH = 2;

    private final String title;
    private final String padding;
    private boolean isFirstExecution = true;

    public Padder(String title, Object... args) {
        this(DEFAULT_PADDING_LENGTH, title, args);
    }

    public Padder(int paddingLength, String title, Object... args) {
        title = title.trim();
        for (Object arg : args) {
            title = title.replaceFirst("\\{}", String.valueOf(arg));
        }
        title = StringUtils.truncateStringIfNeeded(title, paddingLength - (TITLE_SPACING_LENGTH + MIN_PADDING_LENGTH));

        int paddingCount = (paddingLength - (title.length() + TITLE_SPACING_LENGTH)) / 2;
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
