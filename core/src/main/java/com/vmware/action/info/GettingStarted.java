package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays initial info information.")
public class GettingStarted extends AbstractAction {

    public GettingStarted(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Padder gettingStartedTitle = new Padder("Getting Started");
        gettingStartedTitle.infoTitle();
        log.info("This program utilizes the approach of workflows");
        log.info("A workflow consists of workflow actions");
        log.info("e.g. java -jar workflow.jar commit");
        log.info("This workflow consist of actions for setting the summary, description, bug number, testing done etc, and creating the git commit");

        log.info("");
        log.info("For a full listing of all workflow actions and configuration options");
        log.info("run java -jar workflow.jar help");
        log.info("To see what actions will be run, use the flag -dr to do a dry run");
        log.info("e.g. java -jar workflow.jar commit -dr");
        gettingStartedTitle.infoTitle();
    }
}
