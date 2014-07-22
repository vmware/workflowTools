package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@ActionDescription("Displays a predefined list of the main workflows.")
public class DisplayMainWorkflows extends AbstractAction {

    public DisplayMainWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        List<String> mainWorkflows = Arrays.asList("commit", "commitAll", "amendCommit", "commitOffline",
                "closeOldReviews", "pushable", "pushIt", "pushItHarder", "restartJobs", "review");

        Padder mainWorkflowsPadder = new Padder("Main Workflows");
        mainWorkflowsPadder.infoTitle();
        for (String mainWorkflow : mainWorkflows) {
            log.info("{} Actions {}", mainWorkflow, Arrays.toString(config.workflows.get(mainWorkflow)));
        }
        mainWorkflowsPadder.infoTitle();
    }
}
