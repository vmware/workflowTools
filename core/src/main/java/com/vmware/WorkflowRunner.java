package com.vmware;

import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

import com.vmware.util.logging.WorkflowConsoleHandler;
import org.slf4j.LoggerFactory;

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
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        if (Arrays.stream(globalLogger.getHandlers()).noneMatch(handler -> handler instanceof WorkflowConsoleHandler)) {
            LogManager.getLogManager().reset();
            globalLogger.addHandler(new WorkflowConsoleHandler());
        } else {
            LoggerFactory.getLogger(this.getClass()).trace("{} already added as handler", WorkflowConsoleHandler.class.getSimpleName());
        }

        Workflow workflow = new Workflow(appClassLoader, args);
        workflow.runWorkflow();
    }
}
