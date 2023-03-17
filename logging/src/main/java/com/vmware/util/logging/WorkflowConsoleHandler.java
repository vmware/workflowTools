package com.vmware.util.logging;

import java.io.OutputStream;
import java.util.logging.Level;

public class WorkflowConsoleHandler extends java.util.logging.ConsoleHandler {
    public WorkflowConsoleHandler() {
        setFormatter(new SimpleLogFormatter());
        setLevel(Level.FINEST);
    }

    @Override
    protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
        super.setOutputStream(System.out);
    }
}
