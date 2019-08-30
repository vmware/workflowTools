package com.vmware.action.info;

import com.vmware.Workflow;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;

import java.util.Arrays;
import java.util.List;

@ActionDescription("Displays a predefined list of the main workflows.")
public class DisplayMainWorkflows extends BaseAction {

    public DisplayMainWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        printWorkflows("Batch", Workflow.BATCH_MAIN_WORKFLOWS);
        printWorkflows("Git", Workflow.GIT_MAIN_WORKFLOWS);
        printWorkflows("Perforce", Workflow.PERFORCE_MAIN_WORKFLOWS);
        printWorkflows("Vapp", Workflow.VAPP_MAIN_WORKFLOWS);
    }

    protected void printWorkflows(String workflowType, List<String> workflows) {
        Padder mainWorkflowsPadder = new Padder(workflowType + " Workflows");
        mainWorkflowsPadder.infoTitle();
        for (String mainWorkflow : workflows) {
            log.info("{} -> {}", mainWorkflow, config.workflows.get(mainWorkflow).toString());
        }
        mainWorkflowsPadder.infoTitle();
    }
}
