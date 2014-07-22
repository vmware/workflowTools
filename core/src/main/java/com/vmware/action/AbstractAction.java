package com.vmware.action;

import com.vmware.Git;
import com.vmware.config.WorkflowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

public abstract class AbstractAction {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected WorkflowConfig config;

    protected Git git = new Git();

    public AbstractAction(WorkflowConfig config) {
        this.config = config;
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
