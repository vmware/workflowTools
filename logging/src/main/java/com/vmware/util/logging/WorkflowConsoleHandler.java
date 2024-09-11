package com.vmware.util.logging;

import java.io.OutputStream;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class WorkflowConsoleHandler extends java.util.logging.ConsoleHandler {
    private boolean redirectErrorOutputToSystemOut;

    public WorkflowConsoleHandler() {
        this(Level.INFO);
    }

    public WorkflowConsoleHandler(Level level) {
        setFormatter(new SimpleLogFormatter(level));
        this.setLevel(level);
    }

    public void setRedirectErrorOutputToSystemOut(boolean redirectErrorOutputToSystemOut) {
        this.redirectErrorOutputToSystemOut = redirectErrorOutputToSystemOut;
    }

    @Override
    public synchronized void setLevel(Level level) throws SecurityException {
        if (!level.equals(this.getLevel())) {
            super.setFormatter(new SimpleLogFormatter(level));
        }
        super.setLevel(level);
    }

    @Override
    protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
        super.setOutputStream(System.out);
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        if (redirectErrorOutputToSystemOut) {
            super.publish(record);
            return;
        }

        if (getFormatter() == null) {
            setFormatter(new SimpleLogFormatter(getLevel()));
        }

        try {
            String message = getFormatter().format(record);
            if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                System.err.write(message.getBytes());
            } else {
                System.out.write(message.getBytes());
            }
        } catch (Exception exception) {
            reportError(null, exception, ErrorManager.FORMAT_FAILURE);
        }

    }

    @Override
    public void flush() {
        if (redirectErrorOutputToSystemOut) {
            super.flush();
        }
    }
}
