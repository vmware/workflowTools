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
import java.util.Set;

@ActionDescription("Displays a predefined list of the main workflows.")
public class DisplayMainWorkflows extends BaseAction {

    public DisplayMainWorkflows(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        config.mainWorkflowHelpMessages.entrySet().forEach(this::printWorkflows);
    }

    protected void printWorkflows(Map.Entry<String, Map<String, String>> workflows) {
        Padder mainWorkflowsPadder = new Padder(workflows.getKey() + " Workflows");
        mainWorkflowsPadder.infoTitle();
        for (Map.Entry<String, String> entry : workflows.getValue().entrySet()) {
            log.info("{} - {}", entry.getKey(), entry.getValue());
        }
        mainWorkflowsPadder.infoTitle();
    }
}
