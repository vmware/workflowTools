package com.vmware.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.action.BaseAction;
import com.vmware.util.exception.UnknownWorkflowValueException;

/**
 * Used to generate the list of actions to run.
 */
public class WorkflowActions {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WorkflowConfig workflowConfig;
    private List<Class<? extends BaseAction>> workflowActions;

    public WorkflowActions(WorkflowConfig workflowConfig) {
        this.workflowConfig = workflowConfig;
        workflowActions = new WorkflowActionLister().findWorkflowActions();
    }

    public List<Class<? extends BaseAction>> getWorkflowActions() {
        return workflowActions;
    }

    public List<Class<? extends BaseAction>> determineActions(String workflowString) {
        Map<String, List<String>> workflows = workflowConfig.workflows;
        List<String> possibleActions = workflows.get(workflowString);
        if (possibleActions != null) {
            log.info("Using workflow {}", workflowString);
            log.debug("Using workflow values {}", possibleActions.toString());
        } else {
            log.info("Treating workflow argument {} as a custom workflow string as it did not match any existing workflows", workflowString);
            possibleActions = Arrays.asList(workflowString.split(","));
        }
        log.info("");

        WorkflowValuesParser valuesParser = new WorkflowValuesParser(workflows, workflowActions);
        valuesParser.parse(possibleActions);
        workflowConfig.applyConfigValues(valuesParser.getConfigValues(), "Config in Workflow", false);
        if (!valuesParser.getUnknownActions().isEmpty()) {
            throw new UnknownWorkflowValueException(valuesParser.getUnknownActions());
        }
        return valuesParser.getActionClasses();
    }
}
