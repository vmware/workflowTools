package com.vmware;

import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;

import com.vmware.util.logging.SimpleLogFormatter;

/**
 * Class that starts the workflow tools app.
 */
public class WorkflowRunner implements AppLauncher {

    public static void main(String[] args) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        new WorkflowRunner().run(classLoader, Arrays.asList(args));
    }

    @Override
    public void run(ClassLoader appClassLoader, List<String> args) {
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        globalLogger.addHandler(createHandler());

        Workflow workflow = new Workflow(appClassLoader, args);
        workflow.runWorkflow();
    }

    private static ConsoleHandler createHandler() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleLogFormatter());
        handler.setLevel(Level.FINEST);
        return handler;
    }
}
