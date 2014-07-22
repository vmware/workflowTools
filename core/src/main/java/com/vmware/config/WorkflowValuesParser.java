package com.vmware.config;

import com.vmware.action.AbstractAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to parse actions and config values from workflow arguments.
 */
public class WorkflowValuesParser {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private List<Class<? extends AbstractAction>> actionClasses = new ArrayList<Class<? extends AbstractAction>>();
    private Map<String, String> configValues = new HashMap<String, String>();
    private List<String> unknownActions = new ArrayList<String>();

    private Map<String, String[]> workflows;
    private List<Class<? extends AbstractAction>> workFlowActions;

    public WorkflowValuesParser(Map<String, String[]> workflows, List<Class<? extends AbstractAction>> workFlowActions) {
        this.workflows = workflows;
        this.workFlowActions = workFlowActions;
    }

    public void reset() {
        actionClasses.clear();
        configValues.clear();
        unknownActions.clear();
    }

    public void parse(String[] workflowValues) throws ClassNotFoundException, IllegalAccessException {
        for (String workflowValue : workflowValues) {
            if (workflowValue.startsWith("-")) {
                String[] configPieces = workflowValue.split("=");
                // assuming that the config value is boolean if no value specified
                String fieldValue = configPieces.length < 2 ? Boolean.TRUE.toString() : configPieces[1];
                configValues.put(configPieces[0], fieldValue);
                continue;
            }

            if (workflows.containsKey(workflowValue)) {
                parse(workflows.get(workflowValue));
                continue;
            }
            boolean found = false;
            List<String> allActionNames = new ArrayList<String>();
            for (Class<? extends AbstractAction> action : workFlowActions) {
                allActionNames.add(action.getSimpleName());
                if (action.getSimpleName().equals(workflowValue)) {
                    actionClasses.add(action);
                    found = true;
                }
            }
            if (found) {
                log.trace("Found action class {}", workflowValue);
                continue;
            }
            unknownActions.add(workflowValue);
        }
    }

    public List<Class<? extends AbstractAction>> getActionClasses() {
        return actionClasses;
    }

    public Map<String, String> getConfigValues() {
        return configValues;
    }

    public List<String> getUnknownActions() {
        return unknownActions;
    }
}
