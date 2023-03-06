package com.vmware.config;

import com.vmware.action.BaseAction;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Used to generate the list of actions to run.
 */
public class WorkflowActions {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WorkflowConfig workflowConfig;
    private List<Class<? extends BaseAction>> workflowActionClasses;

    public WorkflowActions(WorkflowConfig workflowConfig, ClassLoader classLoader) {
        this.workflowConfig = workflowConfig;
        workflowActionClasses = new WorkflowActionLister().findWorkflowActions(classLoader);
    }

    public List<Class<? extends BaseAction>> getWorkflowActionClasses() {
        return workflowActionClasses;
    }

    public List<WorkflowAction> determineActions(String workflowString) {
        List<String> possibleActions = workflowConfig.workflows.get(workflowString);
        DynamicLogger dynamicLogger = new DynamicLogger(log);
        LogLevel logLevelToUse = workflowConfig.scriptMode ? LogLevel.DEBUG : LogLevel.INFO;
        if (possibleActions != null) {
            dynamicLogger.log(logLevelToUse, "Using workflow {}", workflowString);
            log.debug("Using workflow values {}", possibleActions.toString());
        } else {
            dynamicLogger.log(logLevelToUse, "Using custom workflow argument {}", workflowString);
            possibleActions = Arrays.asList(workflowString.split(","));
        }
        log.debug("");

        WorkflowValuesParser valuesParser = new WorkflowValuesParser(workflowConfig, workflowActionClasses);
        valuesParser.parse(null, possibleActions, Collections.emptyList());
        workflowConfig.applyConfigValues(valuesParser.getConfigValues(), "Config in Workflow", false);
        workflowConfig.applyRuntimeArguments();
        workflowConfig.applyReplacementVariables();
        if (!valuesParser.getUnknownActions().isEmpty() && !workflowConfig.ignoreUnknownActions) {
            throw new UnknownWorkflowValueException(valuesParser.getUnknownActions());
        } else if (!valuesParser.getUnknownActions().isEmpty()) {
            log.warn("Ignoring unknown workflow actions {}", valuesParser.getUnknownActions());
        }
        return valuesParser.getWorkflowActions();
    }
}
