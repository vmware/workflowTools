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
import com.vmware.config.section.FileSystemConfig;
import com.vmware.mapping.ConfigMappings;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.exception.SkipActionException;
import org.slf4j.LoggerFactory;

import static com.vmware.util.StringUtils.pluralizeDescription;
import static java.util.Arrays.asList;

public class WorkflowAction implements Action {
    private final String sectionName;
    private final WorkflowConfig config;
    private final Class<? extends BaseAction> actionClass;
    private final List<Class<? extends BaseAction>> actionClassesToCheck;

    private final List<WorkflowParameter> overriddenConfigValues = new ArrayList<>();

    private BaseAction instantiatedAction;


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
            FileSystemConfig fileSystemConfig = config.fileSystemConfig;
            if (StringUtils.isNotBlank(fileSystemConfig.outputVariableName) && !config.replacementVariables.hasVariable(fileSystemConfig.outputVariableName)) {
                LoggerFactory.getLogger(actionClass).debug("Setting empty value for variable {}", fileSystemConfig.outputVariableName);
                config.replacementVariables.addVariable(fileSystemConfig.outputVariableName, "");
            }
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
        Set<String> unknownParameters = parameters.stream().map(WorkflowParameter::getName)
                .filter(name -> !paramIsAllowed(allowedConfigValues, name)).collect(Collectors.toSet());

        if (!unknownParameters.isEmpty()) {
            Set<String> allConfigValues = mappings.allConfigValues();
            Set<String> completelyUnknownParams = unknownParameters.stream().filter(unknownParam -> !allConfigValues.contains(unknownParam)).collect(Collectors.toSet());
            if (!completelyUnknownParams.isEmpty()) {
                throw new FatalException("Unknown {} {} for action {}",
                        pluralizeDescription(completelyUnknownParams.size(), "parameter"), completelyUnknownParams, getActionClassName());
            }
        };
        for (WorkflowParameter parameter : parameters) {
            if (unknownParameters.contains(parameter.getName())) {
                continue;
            }
            if (overriddenConfigValues.stream().noneMatch(param -> param.getName().equals(parameter.getName()))) {
                overriddenConfigValues.add(parameter);
            } else {
                LoggerFactory.getLogger(this.getClass()).debug("Config value {} already overridden", parameter.getName());
            }
        }
    }

    private boolean paramIsAllowed(Set<String> allowedConfigValues, String propertyName) {
        if (WorkflowFields.isSystemProperty(propertyName)) {
            return true;
        }
        return allowedConfigValues.contains(propertyName);
    }
}
