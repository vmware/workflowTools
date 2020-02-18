package com.vmware.action.info;

import java.util.List;
import java.util.Set;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.mapping.ConfigValuesCompleter;
import com.vmware.util.StringUtils;

@ActionDescription("Generates a list of auto complete values for the specified workflow. Intended for auto complete support only.")
public class GenerateAutoCompleteValues extends BaseAction {
    public GenerateAutoCompleteValues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(commandLineConfig.autocompleteWorkflow)) {
            Set<String> workflows = config.workflows.keySet();
            List<String> supportingWorkflows = config.supportingWorkflows;
            workflows.removeAll(supportingWorkflows);
            System.out.println(StringUtils.join(workflows, " "));
        } else {
            ConfigValuesCompleter completer = new ConfigValuesCompleter(config);
            System.out.println(StringUtils.join(completer.generateValuesForWorkflowString(commandLineConfig.autocompleteWorkflow), " "));
        }
    }
}
