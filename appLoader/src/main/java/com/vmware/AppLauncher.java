package com.vmware;

import java.util.List;

/**
 * Main class for jar application
 */
public interface AppLauncher {
    String WORKFLOW_JAR = "WORKFLOW_JAR";
    /**
     * Launches application
     *
     * @param appClassLoader class loader to use for the application jar file.
     * @param args command line arguments passed in
     */
    void run(ClassLoader appClassLoader, List<String> args);
}
