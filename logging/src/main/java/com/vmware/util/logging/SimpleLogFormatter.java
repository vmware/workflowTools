package com.vmware.util.logging;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * JDK Logger formatter. Used to format all log output.
 * For info and severe log level, the class and method name are not printed.
 * For all levels lower than info, the class and method name are printed.
 */
public class SimpleLogFormatter extends Formatter {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss:SSS");

    public String format(LogRecord record) {

        Level logLevel = Logger.getLogger("com.vmware").getLevel();
        if (record.getMessage().isEmpty() && logLevel != Level.FINER && logLevel != Level.FINEST) {
            return System.lineSeparator();
        }

        // Create a StringBuilder to contain the formatted record
        StringBuilder sb = new StringBuilder();

        if (logLevel == Level.FINER || logLevel == Level.FINEST) {
            String className = record.getSourceClassName();
            if (className.contains(".")) {
                className = className.substring(className.lastIndexOf(".") + 1);
            }
            sb.append(dateFormat.format(record.getMillis())).append(" ").append(className).append(".").append(record.getSourceMethodName()).append(" ");
        }

        if (logLevel != Level.INFO || (record.getLevel() == Level.WARNING || record.getLevel() == Level.SEVERE)) {
            // Get the level name and add it to the buffer
            String levelString = LogLevel.fromLevel(record.getLevel()).name();
            sb.append(levelString);
            sb.append(": ");
        }

        // Get the formatted message (includes localization
        // and substitution of parameters) and add it to the buffer
        sb.append(formatMessage(record));
        sb.append(System.lineSeparator());

        return sb.toString();
    }
}
