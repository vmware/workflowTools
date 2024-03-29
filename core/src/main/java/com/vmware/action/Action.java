package com.vmware.action;

import java.io.IOException;

public interface Action {
    /**
     * Setup method that will run asynchonrously, useful for setting up rest services
     */
    void asyncSetup();

    void checkIfWorkflowShouldBeFailed();

    /**
     * @return Reason for why this action should not be run, null if it should be run
     */
    void checkIfActionShouldBeSkipped();

    /**
     * Override if any setup is needed before the process method is called
     */
    void preprocess();

    /**
     * Performs the actual action
     */
    void process();
}
