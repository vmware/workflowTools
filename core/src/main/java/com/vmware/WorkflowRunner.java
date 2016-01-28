package com.vmware;

import java.util.logging.LogManager;

/**
 * Class that starts the workflow tools app.
 */
public class WorkflowRunner {

    public static void main(String[] args) {
        System.setProperty("jsse.enableSNIExtension", "false");
        LogManager.getLogManager().reset();

        Workflow workflow = new Workflow();
        workflow.init(args);
        workflow.runWorkflow();
    }
}
