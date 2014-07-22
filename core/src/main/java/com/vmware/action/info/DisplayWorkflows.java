package com.vmware.action.info;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ActionDescription("Displays a list of workflows that are configured either in the default config file or in an external config file.")
public class DisplayWorkflows extends AbstractAction {

    public DisplayWorkflows(WorkflowConfig config) {
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

        Padder additionalWorkflowsPadder = new Padder("Additional Workflows");
        additionalWorkflowsPadder.infoTitle();
        Set<String> sortedWorkflows = new TreeSet<String>(config.workflows.keySet());
        for (String workflow : sortedWorkflows) {
            if (!mainWorkflows.contains(workflow)) {
                log.info("{} Actions {}", workflow, Arrays.toString(config.workflows.get(workflow)));
            }
        }
        additionalWorkflowsPadder.infoTitle();
    }
}
