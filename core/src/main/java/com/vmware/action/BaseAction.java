package com.vmware.action;

import com.vmware.scm.Git;
import com.vmware.ServiceLocator;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAction {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final WorkflowConfig config;

    protected final ServiceLocator serviceLocator;

    protected final Git git;

    private String[] expectedCommandsToBeAvailable;


    public BaseAction(WorkflowConfig config) {
        this.config = config;
        this.serviceLocator = config.configuredServiceLocator();
        this.git = serviceLocator.getGit();
    }

    /**
     * @return Reason why the workflow should fail, null if it should continue
     */
    public String failWorkflowIfConditionNotMet() {
        if (expectedCommandsToBeAvailable == null) {
            return null;
        }
        for (String command : expectedCommandsToBeAvailable) {
            if (!CommandLineUtils.isCommandAvailable(command)) {
                return "command " + command + " is not available";
            }
        }
        return null;
    }

    protected String perforceClientCanBeUsed() {
        if (!CommandLineUtils.isCommandAvailable("p4")) {
            return "p4 command is not availabled";
        }
        if (StringUtils.isBlank(config.perforceClientName)) {
            config.perforceClientName = serviceLocator.getPerforce().getClientName();
        }
        return null;
    }

    /**
     * Setup method that will run asynchonrously, useful for setting up rest services
     */
    public void asyncSetup() {
    }

    /**
     * @return Reason for why this action should not be run, null if it should be run
     */
    public String cannotRunAction() {
        return null;
    }

    /**
     * Override if any setup is needed before the process method is called
     */
    public void preprocess() {
    }

    public abstract void process();

    protected void setExpectedCommandsToBeAvailable(String... commands) {
        this.expectedCommandsToBeAvailable = commands;
    }

}
