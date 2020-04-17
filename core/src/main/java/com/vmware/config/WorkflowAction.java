package com.vmware.config;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vmware.ServiceLocator;
import com.vmware.action.Action;
import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.base.BaseIssuesProcessingAction;
import com.vmware.action.base.BaseVappAction;
import com.vmware.action.trello.BaseTrelloAction;
import com.vmware.util.exception.SkipActionException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;

public class WorkflowAction implements Action {
    private WorkflowConfig config;
    private Class<? extends BaseAction> actionClass;
    private BaseAction instantiatedAction;

    private List<WorkflowParameter> overriddenConfigValues;

    private Map<String, CalculatedProperty> existingValuesForConfig;

    public WorkflowAction(WorkflowConfig config, Class<? extends BaseAction> actionClass, List<WorkflowParameter> parameters) {
        this.config = config;
        this.actionClass = actionClass;
        this.overriddenConfigValues = parameters;
    }

    public String getActionClassName() {
        return actionClass.getSimpleName();
    }

    public ActionDescription getActionDescription() {
        return actionClass.getAnnotation(ActionDescription.class);
    }

    public void instantiateAction(WorkflowConfig config, ServiceLocator serviceLocator) {
        try {
            instantiatedAction = actionClass.getConstructor(WorkflowConfig.class).newInstance(config);
            instantiatedAction.setServiceLocator(serviceLocator);
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    @Override
    public void asyncSetup() {
        instantiatedAction.asyncSetup();
    }

    @Override
    public void process() {
        instantiatedAction.process();
        resetConfigValues();
    }

    @Override
    public void preprocess() {
        instantiatedAction.preprocess();
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        if (!overriddenConfigValues.isEmpty()) {
            Map<String, String> paramsMap = overriddenConfigValues.stream().collect(Collectors.toMap(WorkflowParameter::getName, WorkflowParameter::getValue));
            existingValuesForConfig = config.getExistingValues(paramsMap.keySet());
            config.applyConfigValues(paramsMap, actionClass.getSimpleName(), true);
            config.setupLogLevel();
        }
        try {
            instantiatedAction.checkIfActionShouldBeSkipped();
        } catch (SkipActionException ce) {
            resetConfigValues();
            throw ce;
        }
    }

    @Override
    public void checkIfWorkflowShouldBeFailed() {
        instantiatedAction.checkIfWorkflowShouldBeFailed();
    }

    public void setWorkflowValuesOnAction(WorkflowActionValues values) {
        if (instantiatedAction instanceof BaseCommitAction) {
            ((BaseCommitAction) instantiatedAction).setDraft(values.getDraft());
        }
        if (instantiatedAction instanceof BaseIssuesProcessingAction) {
            ((BaseIssuesProcessingAction) instantiatedAction).setProjectIssues(values.getProjectIssues());
        }
        if (instantiatedAction instanceof BaseTrelloAction) {
            ((BaseTrelloAction) instantiatedAction).setSelectedBoard(values.getTrelloBoard());
        }
        if (instantiatedAction instanceof BaseVappAction) {
            ((BaseVappAction) instantiatedAction).setVappData(values.getVappData());
        }
    }

    public void updateWorkflowValues(WorkflowActionValues values) {
        if (instantiatedAction instanceof BaseTrelloAction) {
            values.setTrelloBoard(((BaseTrelloAction) instantiatedAction).getSelectedBoard());
        }
    }

    public List<WorkflowParameter> getRelevantOverriddenConfigValues(Set<String> relevantConfigValues) {
        return overriddenConfigValues.stream().filter(param -> relevantConfigValues.contains(param.getName())).collect(Collectors.toList());
    }

    public Set<String> getConfigValues(Map<String, List<String>> mappings) {
        Set<String> configValues = new HashSet<String>();
        Class classToGetValuesFor = actionClass;
        while (classToGetValuesFor != Object.class) {
            List<String> configValuesForClass = mappings.get(classToGetValuesFor.getSimpleName());
            if (configValuesForClass != null) {
                configValues.addAll(configValuesForClass);
            }
            boolean ignoreSuperClass = false;
            if (classToGetValuesFor != BaseAction.class && classToGetValuesFor.isAnnotationPresent(ActionDescription.class)) {
                Class<? extends BaseAction> actionClass = classToGetValuesFor;
                ignoreSuperClass = actionClass.getAnnotation(ActionDescription.class).ignoreConfigValuesInSuperclass();
            }
            classToGetValuesFor = ignoreSuperClass ? Object.class : classToGetValuesFor.getSuperclass();
        }
        return configValues;
    }

    private void resetConfigValues() {
        if (!overriddenConfigValues.isEmpty()) {
            config.applyValuesWithSource(existingValuesForConfig);
            config.setupLogLevel();
        }
    }
}
