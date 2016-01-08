package com.vmware.action;

import com.vmware.Git;
import com.vmware.ServiceLocator;
import com.vmware.config.WorkflowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public abstract class BaseAction {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final WorkflowConfig config;

    protected final ServiceLocator serviceLocator;

    protected final Git git = new Git();


    public BaseAction(WorkflowConfig config) {
        this.config = config;
        this.serviceLocator = config.configuredServiceLocator();
    }

    /**
     * @return Whether this action should be run
     */
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        return true;
    }

    /**
     * Override if any pre process setup is needed
     */
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {

    }

    public abstract void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException;

}
