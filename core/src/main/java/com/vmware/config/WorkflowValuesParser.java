package com.vmware.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.vmware.action.BaseAction;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to parse actions and config values from workflow arguments.
 */
public class WorkflowValuesParser {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private Map<String, String> configValues = new HashMap<>();
    private List<String> unknownActions = new ArrayList<>();

    private final WorkflowConfig config;
    private Map<String, List<String>> workflows;
    private List<WorkflowAction> workflowActions = new ArrayList<>();
    private List<Class<? extends BaseAction>> workflowActionClasses;
    private JenkinsConfig jenkinsConfig;

    public WorkflowValuesParser(WorkflowConfig workflowConfig, List<Class<? extends BaseAction>> workflowActionClasses) {
        this.config = workflowConfig;
        this.workflows = workflowConfig.workflows;
        this.workflowActionClasses = workflowActionClasses;
        this.jenkinsConfig = workflowConfig.jenkinsConfig;
    }

    public void parse(List<String> workflowValues, List<WorkflowParameter> workflowParameters) {
        for (String workflowValue : workflowValues) {
            String[] workflowPieces = workflowValue.split("&&-");
            String workflowName = workflowPieces[0];
            workflowPieces[0] = null;
            List<WorkflowParameter> parameters = Arrays.stream(workflowPieces).filter(Objects::nonNull)
                    .map(value -> "-" + value).map(WorkflowParameter::new).collect(Collectors.toList());
            parameters.addAll(workflowParameters);

            if (workflowName.startsWith("-")) {
                WorkflowParameter parameter = new WorkflowParameter(workflowName);
                configValues.put(parameter.getName(), parameter.getValue());
                parameters.forEach(param -> configValues.put(param.getName(), param.getValue()));
                continue;
            }

            if (workflows.containsKey(workflowName)) {
                parse(workflows.get(workflowName), parameters);
                continue;
            }

            Optional<Class<? extends BaseAction>> matchingAction = workflowActionClasses.stream()
                    .filter(action -> action.getSimpleName().equals(workflowName)).findFirst();
            if (matchingAction.isPresent()) {
                workflowActions.add(new WorkflowAction(config, matchingAction.get(), parameters));
                log.trace("Found action class {}", workflowName);
            } else {
                unknownActions.add(workflowName);
            }
        }
    }

    public List<WorkflowAction> getWorkflowActions() {
        return workflowActions;
    }

    public Map<String, String> getConfigValues() {
        return configValues;
    }

    public List<String> getUnknownActions() {
        return unknownActions;
    }

    public Collection<? extends String> calculateJenkinsParameterConfigValues() {
        String jenkinsJobsToCall = configValues.get("-j");
        if (StringUtils.isBlank(jenkinsJobsToCall)) {
            jenkinsJobsToCall = configValues.get("--jenkins-jobs");
        }
        if (StringUtils.isBlank(jenkinsJobsToCall)) {
            return Collections.emptyList();
        }
        String[] jenkinsJobs = jenkinsJobsToCall.split(",");
        Set<String> jenkinsParameterConfigValues = new HashSet<>();
        for (String job : jenkinsJobs) {
            jenkinsParameterConfigValues.addAll(determineParametersForJob(job));
        }

        return jenkinsParameterConfigValues;
    }

    private Collection<? extends String> determineParametersForJob(String jobText) {
        if (jenkinsConfig.jenkinsJobsMappings.containsKey(jobText)) {
            jobText = jenkinsConfig.jenkinsJobsMappings.get(jobText);
        }
        String[] jenkinsJobPieces = jobText.split("&");
        if (jenkinsJobPieces.length < 2) {
            return Collections.emptyList();
        }
        Set<String> jenkinsParameterConfigValues = new HashSet<>();
        for (int i = 1; i < jenkinsJobPieces.length; i++) {
            String jenkinsParameter = jenkinsJobPieces[i];
            String[] jenkinsParameterPieces = StringUtils.splitOnlyOnce(jenkinsParameter, "=");
            if (jenkinsParameterPieces.length != 2) {
                continue;
            }
            jenkinsParameterConfigValues.add("--J" + jenkinsParameterPieces[0]);
        }
        return jenkinsParameterConfigValues;
    }
}
