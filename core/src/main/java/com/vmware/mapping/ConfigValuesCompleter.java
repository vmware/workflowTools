package com.vmware.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.vmware.Workflow;
import com.vmware.action.BaseAction;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowAction;
import com.vmware.config.WorkflowActions;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowValuesParser;
import com.vmware.util.CollectionUtils;
import com.vmware.util.input.ArgumentListAware;
import com.vmware.util.input.ImprovedStringsCompleter;

import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completer for JLine2.
 * Allows tab completion of workflow config values on the command line.
 */
public class ConfigValuesCompleter extends ImprovedStringsCompleter implements Completer, ArgumentListAware {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private ConfigMappings configMappings;
    private ArgumentCompleter.ArgumentList argumentList;
    private WorkflowConfig config;
    private List<Class<? extends BaseAction>> workflowActions;

    public ConfigValuesCompleter(WorkflowConfig config) {
        this.configMappings = new ConfigMappings();
        this.config = config;
        this.workflowActions = new WorkflowActions(config, WorkflowConfig.realClassLoader).getWorkflowActionClasses();
        super.values.addAll(configMappings.allConfigValues());
    }

    @Override
    public int complete(final String buffer, final int cursor, final List<CharSequence> candidates) {
        if (argumentList == null || argumentList.getArguments().length == 0) {
            return super.complete(buffer, cursor, candidates);
        }

        String workflowString = argumentList.getArguments()[0];
        values.clear();
        valuesShownWhenNoBuffer.clear();
        values.addAll(generateValuesForWorkflowString(workflowString));
        valuesShownWhenNoBuffer.addAll(values);
        return super.complete(buffer, cursor, candidates);
    }

    public SortedSet<String> generateValuesForWorkflowString(String workflowString) {
        SortedSet<String> values = new TreeSet<>();
        Set<String> valuesToRemove = new HashSet<>();
        WorkflowValuesParser valuesParser = new WorkflowValuesParser(config, workflowActions);
        valuesParser.parse(null, Arrays.asList(workflowString.split(",")), Collections.emptyList());
        for (WorkflowAction foundAction : valuesParser.getWorkflowActions()) {
            Set<String> matchingConfigValues = configMappings.getUsableConfigValuesForAction(foundAction);
            valuesToRemove.addAll(foundAction.configFlagsToAlwaysRemoveFromCompleter());
            matchingConfigValues.removeIf(values::contains);
            if (CollectionUtils.isNotEmpty(matchingConfigValues)) {
                log.trace("Action {} added {} config flags", foundAction.getActionClassName(), matchingConfigValues);
            }
            values.addAll(matchingConfigValues);
        }
        values.addAll(valuesParser.calculateJenkinsParameterConfigValues());
        values.addAll(valuesParser.calculateReplacementVariables());

        values.removeAll(valuesToRemove);
        removeUnneededConfigValues(values);
        return values;
    }

    private void removeUnneededConfigValues(SortedSet<String> values) {
        // means directory is a git repo so perforce changelist id is not needed
        if (config.replacementVariables.hasVariable(ReplacementVariables.VariableName.REPO_DIR)) {
            values.remove("--changelist-id");
        }
        values.remove("--username"); // no one really uses it, can still be accessed
    }

    @Override
    public void setArgumentList(ArgumentCompleter.ArgumentList argumentList) {
        this.argumentList = argumentList;
    }
}
