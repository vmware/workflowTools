package com.vmware.config;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.vmware.mapping.ConfigMappings;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.exception.SkipActionException;

import static com.vmware.util.StringUtils.pluralizeDescription;
import static java.util.Arrays.asList;

public class WorkflowAction implements Action {
    private String sectionName;
    private WorkflowConfig config;
    private Class<? extends BaseAction> actionClass;
    private List<Class<? extends BaseAction>> actionClassesToCheck;
    private BaseAction instantiatedAction;

    private List<WorkflowParameter> overriddenConfigValues;

    private Map<String, CalculatedProperty> existingValuesForConfig;

    public WorkflowAction(String sectionName, ConfigMappings mappings, WorkflowConfig config, Class<? extends BaseAction> actionClass, List<WorkflowParameter> parameters) {
        this.sectionName = sectionName;
        this.config = config;
        this.actionClass = actionClass;
        this.actionClassesToCheck = ReflectionUtils.collectClassHierarchyInDescendingOrder(actionClass).stream()
                .map(clazz -> (Class<? extends BaseAction>) clazz).collect(Collectors.toList());
        setWorkflowParametersForAction(mappings, parameters);
    }

    public String getActionClassName() {
        return actionClass.getSimpleName();
    }

    public List<String> configFlagsToRemoveFromCompleter() {
        return actionClassesToCheck.stream().filter(clazz -> clazz.isAnnotationPresent(ActionDescription.class))
                .map(actionClazz -> actionClazz.getAnnotation(ActionDescription.class).configFlagsToExcludeFromCompleter())
                .flatMap(Arrays::stream).collect(Collectors.toList());
    }

    public List<String> configFlagsToAlwaysRemoveFromCompleter() {
        return actionClassesToCheck.stream().filter(clazz -> clazz.isAnnotationPresent(ActionDescription.class))
                .map(actionClazz -> actionClazz.getAnnotation(ActionDescription.class).configFlagsToAlwaysExcludeFromCompleter())
                .flatMap(Arrays::stream).collect(Collectors.toList());
    }

    public String getSectionName() {
        return sectionName;
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
        config.addGeneratedVariables();
        if (!overriddenConfigValues.isEmpty()) {
            Map<String, String> paramsMap = overriddenConfigValues.stream().collect(Collectors.toMap(WorkflowParameter::getName, WorkflowParameter::getValue));
            existingValuesForConfig = config.getExistingValues(paramsMap.keySet());
            config.applyConfigValues(paramsMap, actionClass.getSimpleName(), true);
            config.applyReplacementVariables();
            config.setupLogging();
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

    public List<WorkflowParameter> getOverriddenConfigValues() {
        return config.applyReplacementVariables(overriddenConfigValues);
    }

    public Set<String> getWorkflowParameterNames() {
        return overriddenConfigValues.stream().map(WorkflowParameter::getName).collect(Collectors.toSet());
    }

    public Set<String> getConfigValues(Map<String, List<String>> mappings, boolean autoCompleteValuesOnly) {
        Set<String> configValues = new HashSet<>();
        actionClassesToCheck.forEach(clazz -> {
            List<String> configValuesForClass = mappings.get(clazz.getSimpleName());
            if (configValuesForClass != null) {
                configValues.addAll(configValuesForClass);
            }
            if (clazz.isAnnotationPresent(ActionDescription.class)) {
                ActionDescription actionDescription = clazz.getAnnotation(ActionDescription.class);
                if (autoCompleteValuesOnly) {
                    List<String> configFlagsToExclude = asList(actionDescription.configFlagsToExcludeFromCompleter());
                    configValues.removeIf(configFlagsToExclude::contains);
                }
            }
        });
        return configValues;
    }

    private void resetConfigValues() {
        if (!overriddenConfigValues.isEmpty()) {
            config.applyValuesWithSource(existingValuesForConfig);
            config.setupLogging();
        }
    }

    private void setWorkflowParametersForAction(ConfigMappings mappings, List<WorkflowParameter> parameters) {
        Set<String> allowedConfigValues = mappings.getConfigValuesForAction(this, false);
        Set<String> unknownParameters = parameters.stream().filter(param -> !paramIsAllowed(allowedConfigValues, param.getName()))
                .map(WorkflowParameter::getName).collect(Collectors.toSet());

        if (!unknownParameters.isEmpty()) {
            Set<String> allConfigValues = mappings.allConfigValues();
            Set<String> completelyUnknownParams = unknownParameters.stream().filter(unknownParam -> !allConfigValues.contains(unknownParam)).collect(Collectors.toSet());
            if (!completelyUnknownParams.isEmpty()) {
                throw new FatalException("Unknown {} {} for action {}",
                        pluralizeDescription(completelyUnknownParams.size(), "parameter"), completelyUnknownParams, getActionClassName());
            }
        }
        this.overriddenConfigValues = parameters.stream().filter(param -> !unknownParameters.contains(param.getName())).collect(Collectors.toList());
    }

    private boolean paramIsAllowed(Set<String> allowedConfigValues, String propertyName) {
        if (WorkflowFields.isSystemProperty(propertyName)) {
            return true;
        }
        return allowedConfigValues.contains(propertyName);
    }
}
