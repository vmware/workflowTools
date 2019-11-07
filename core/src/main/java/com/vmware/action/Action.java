package com.vmware.action;

public interface Action {
    /**
     * Setup method that will run asynchonrously, useful for setting up rest services
     */
    public void asyncSetup();

    /**
     * @return Reason why the workflow should fail, null if it should continue
     */
    public String failWorkflowIfConditionNotMet();

    /**
     * @return Reason for why this action should not be run, null if it should be run
     */
    public String cannotRunAction();

    /**
     * Override if any setup is needed before the process method is called
     */
    public void preprocess();

    /**
     * Performs the actual action
     */
    public void process();
}
