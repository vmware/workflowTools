package com.vmware.action.info;

import com.vmware.Workflow;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@ActionDescription("Displays all other workflows not categorized as main workflows.")
public class DisplayAdditionalWorkflows extends BaseAction {

    public DisplayAdditionalWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder additionalWorkflowsPadder = new Padder("Additional Workflows");
        additionalWorkflowsPadder.infoTitle();
        Set<String> sortedWorkflows = new TreeSet<String>(config.workflows.keySet());
        List<String> mainWorkflows = new ArrayList<>();
        mainWorkflows.addAll(Workflow.BATCH_MAIN_WORKFLOWS);
        mainWorkflows.addAll(Workflow.GIT_MAIN_WORKFLOWS);
        mainWorkflows.addAll(Workflow.PERFORCE_MAIN_WORKFLOWS);
        for (String workflow : sortedWorkflows) {
            if (!mainWorkflows.contains(workflow) && !config.supportingWorkflows.contains(workflow)) {
                log.info("{} -> {}", workflow, config.workflows.get(workflow).toString());
            }
        }
        additionalWorkflowsPadder.infoTitle();
    }
}
