package com.vmware.config;

import com.vmware.action.BaseAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        List<String> possibleActions = workflowConfig.workflows.get(workflowString);
        if (possibleActions != null) {
            log.info("Using workflow {}", workflowString);
            log.debug("Using workflow values {}", possibleActions.toString());
        } else {
            log.info("Treating workflow argument {} as a custom workflow string as it did not match any existing workflows", workflowString);
            possibleActions = Arrays.asList(workflowString.split(","));
        }
        log.info("");

        WorkflowValuesParser valuesParser = new WorkflowValuesParser(workflowConfig, workflowActions);
        valuesParser.parse(possibleActions);
        workflowConfig.applyConfigValues(valuesParser.getConfigValues(), "Config in Workflow", false);
        if (!valuesParser.getUnknownActions().isEmpty() && !workflowConfig.ignoreUnknownActions) {
            throw new UnknownWorkflowValueException(valuesParser.getUnknownActions());
        } else if (!valuesParser.getUnknownActions().isEmpty()) {
            log.warn("Ignoring unknown workflow actions {}", valuesParser.getUnknownActions());
        }
        return valuesParser.getActionClasses();
    }
}
