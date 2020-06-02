package com.vmware.action.info;

import com.vmware.Workflow;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.ClasspathResource;
import com.vmware.util.logging.Padder;

import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ActionDescription("Displays a predefined list of the main workflows.")
public class DisplayMainWorkflows extends BaseAction {

    public DisplayMainWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        printWorkflows("Git", Workflow.GIT_MAIN_WORKFLOWS);
        printWorkflows("Vapp", Workflow.VAPP_MAIN_WORKFLOWS);
        printWorkflows("Perforce", Workflow.PERFORCE_MAIN_WORKFLOWS);
        printWorkflows("Batch", Workflow.BATCH_MAIN_WORKFLOWS);
    }

    protected void printWorkflows(String workflowType, List<String> workflows) {
        Map<String, String> workflowsHelp = realWorkflowsHelp();
        Padder mainWorkflowsPadder = new Padder(workflowType + " Workflows");
        mainWorkflowsPadder.infoTitle();
        for (String mainWorkflow : workflows) {
            if (!workflowsHelp.containsKey(mainWorkflow)) {
                log.warn("No help found for workflow named {}!", mainWorkflow);
                continue;
            }
            log.info("{} - {}", mainWorkflow, workflowsHelp.get(mainWorkflow));
        }
        mainWorkflowsPadder.infoTitle();
    }

    private Map<String, String> realWorkflowsHelp() {
        Reader reader = new ClasspathResource("/mainWorkflowsHelp.json", this.getClass()).getReader();
        return new ConfiguredGsonBuilder().build().fromJson(reader, Map.class);
    }
}
