package com.vmware.mapping;

import com.vmware.action.BaseAction;
import com.vmware.config.WorkflowAction;
import com.vmware.config.WorkflowActions;
import com.vmware.config.WorkflowValuesParser;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.ArgumentListAware;
import com.vmware.util.input.ImprovedStringsCompleter;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Completer for JLine2.
 * Allows tab completion of workflow config values on the command line.
 */
public class ConfigValuesCompleter extends ImprovedStringsCompleter implements Completer, ArgumentListAware {

    private ConfigMappings configMappings;
    private ArgumentCompleter.ArgumentList argumentList;
    private WorkflowConfig config;
    private List<Class<? extends BaseAction>> workflowActions;

    public ConfigValuesCompleter(WorkflowConfig config) {
        this.configMappings = new ConfigMappings();
        this.config = config;
        this.workflowActions = new WorkflowActions(config).getWorkflowActionClasses();
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
        WorkflowValuesParser valuesParser = new WorkflowValuesParser(config, workflowActions);
        valuesParser.parse(Arrays.asList(workflowString.split(",")), Collections.emptyList());
        for (WorkflowAction foundAction : valuesParser.getWorkflowActions()) {
            Set<String> matchingConfigValues = configMappings.getConfigValuesForAction(foundAction);
            values.addAll(matchingConfigValues);
        }
        values.addAll(valuesParser.calculateJenkinsParameterConfigValues());
        return values;
    }

    @Override
    public void setArgumentList(ArgumentCompleter.ArgumentList argumentList) {
        this.argumentList = argumentList;
    }
}
