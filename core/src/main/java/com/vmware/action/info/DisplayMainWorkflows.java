package com.vmware.action.info;

import com.vmware.Workflow;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

@ActionDescription("Displays a predefined list of the main workflows.")
public class DisplayMainWorkflows extends BaseAction {

    public DisplayMainWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        Padder mainWorkflowsPadder = new Padder("Main Workflows");
        mainWorkflowsPadder.infoTitle();
        for (String mainWorkflow : Workflow.MAIN_WORKFLOWS) {
            log.info("{} -> {}", mainWorkflow, Arrays.toString(config.workflows.get(mainWorkflow)));
        }
        mainWorkflowsPadder.infoTitle();
    }
}
