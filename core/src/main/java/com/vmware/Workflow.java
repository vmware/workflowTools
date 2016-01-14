package com.vmware;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseMultiActionDataSupport;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.trello.BaseTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.UnknownWorkflowValueException;
import com.vmware.config.WorkflowActionValues;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowConfigParser;
import com.vmware.jira.domain.MultiActionData;
import com.vmware.mapping.ConfigMappings;
import com.vmware.mapping.ConfigValuesCompleter;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.IOUtils;
import com.vmware.util.Padder;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.input.CommaArgumentDelimeter;
import com.vmware.util.input.ImprovedArgumentCompleter;
import com.vmware.util.input.ImprovedStringsCompleter;
import com.vmware.util.input.InputUtils;
import jline.console.completer.ArgumentCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main class for running the workflow application.
 */
public class Workflow {
    public static final List<String> MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("commit", "commitAll", "amendCommit", "review",
                    "pushable", "push", "commitOffline", "commitAllOffline",
                    "createTrelloBoardFromLabel" , "closeOldReviews", "restartJobs"));

    private static final String EXIT_WORKFLOW = "exit";

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final WorkflowConfigParser configParser = new WorkflowConfigParser();
    private List<String> workflowHistory;
    private WorkflowConfig config;

    public void init(String[] args) throws IOException, IllegalAccessException {
        readWorkflowHistoryFile();

        config = configParser.parseWorkflowConfig(args);
        askForWorkflowIfEmpty();
    }

    private void updateWorkflowHistoryFile() throws IOException {
        String argumentsText = configParser.getRuntimeArgumentsText();
        if (workflowHistory.isEmpty() || !workflowHistory.get(0).equals(argumentsText)) {
            workflowHistory.add(0, argumentsText);
        }
        if (workflowHistory.size() > 10) {
            workflowHistory.remove(10);
        }
        String userHome = System.getProperty( "user.home" );
        File workflowHistoryFile = new File(userHome + File.separator + ".workflowHistory.txt");
        IOUtils.write(workflowHistoryFile, workflowHistory);
    }

    private void readWorkflowHistoryFile() {
        String userHome = System.getProperty( "user.home" );
        File workflowHistoryFile = new File(userHome + File.separator + ".workflowHistory.txt");
        if (!workflowHistoryFile.exists()) {
            workflowHistory = new ArrayList<>();
        } else {
            workflowHistory = IOUtils.readLines(workflowHistoryFile);
        }
    }


    private void askForWorkflowIfEmpty() {
        if (StringUtils.isNotBlank(config.workflowsToRun)) {
            return;
        }

        askForWorkflow();
    }

    private void askForWorkflow() {
        log.info("Press ENTER for getting started info");
        log.info("");

        log.info("Press tab to see a list of available workflows, up to see previously entered workflows");

        ArgumentCompleter argumentsCompleter = createWorkflowCompleter();

        String workFlowText = InputUtils.readValue("Workflow(Type " + EXIT_WORKFLOW + " to exit)",
                argumentsCompleter, workflowHistory).trim();
        if (workFlowText.equals(EXIT_WORKFLOW)) {
            log.info("Exiting");
            System.exit(0);
        }
        configParser.updateWithRuntimeArguments(config, workFlowText.split(" "));
    }

    private ArgumentCompleter createWorkflowCompleter() {
        List<String> autocompleteList = new ArrayList<String>();

        for (String workflow : config.workflows.keySet()) {
            if (config.supportingWorkflows.contains(workflow)) {
                // ! means that it won't show up if nothing is entered
                autocompleteList.add("!" + workflow);
            } else {
                autocompleteList.add(workflow);
            }
        }

        for (Class workflowAction : config.workFlowActions) {
            // ! means that it won't show up if nothing is entered
            autocompleteList.add("!" + workflowAction.getSimpleName());
        }
        ImprovedStringsCompleter workflowCompleter = new ImprovedStringsCompleter(autocompleteList);
        workflowCompleter.setDelimeterText("");
        workflowCompleter.addValue("!" + EXIT_WORKFLOW);
        ArgumentCompleter workflowArgumentCompleter = new ImprovedArgumentCompleter(new CommaArgumentDelimeter(), workflowCompleter);
        workflowArgumentCompleter.setStrict(false);

        ConfigValuesCompleter configValueCompleter = new ConfigValuesCompleter(config);
        configValueCompleter.setDelimeterText("");
        ArgumentCompleter argumentsCompleter = new ImprovedArgumentCompleter(workflowArgumentCompleter, configValueCompleter);
        argumentsCompleter.setStrict(false);
        return argumentsCompleter;
    }

    public void runWorkflow() throws ClassNotFoundException, IllegalAccessException,
            URISyntaxException, InstantiationException, NoSuchMethodException, InvocationTargetException,
            IOException, ParseException, UnknownWorkflowValueException {

        if (StringUtils.isBlank(config.workflowsToRun)) {
            // default workflow
            config.workflowsToRun = "intro";
        }

        String workflowToRun = config.workflowsToRun;
        if (workflowToRun.equals("abalta") || workflowToRun.equals("anabalta")) {
            checkAllActionsCanBeInstantiated(workflowToRun.equals("anabalta"));
            return;
        }

        try {
            List<Class<? extends BaseAction>> workflowActions = config.determineActions(workflowToRun);
            // update history file after all the workflow has been determined to be valid
            updateWorkflowHistoryFile();
            if (config.dryRun) {
                dryRunActions(workflowActions);
            } else {
                runActions(workflowActions, new WorkflowActionValues());
            }
        } catch (UnknownWorkflowValueException e) {
            log.error(e.getMessage());
            askForWorkflow();
            runWorkflow();
        } catch (IllegalArgumentException iae) {
            log.error(iae.getMessage());
            if (log.isDebugEnabled()) {
                log.debug(iae.getMessage(), iae);
            }
        }
    }

    private void checkAllActionsCanBeInstantiated(boolean runAllHelperMethods)
            throws ClassNotFoundException, IllegalAccessException, UnknownWorkflowValueException, InstantiationException,
            InvocationTargetException, NoSuchMethodException, IOException, URISyntaxException {
        log.info("Checking that each action value in the workflows is valid");
        ReviewRequestDraft draft = new ReviewRequestDraft();
        MultiActionData multiActionData = new MultiActionData();
        for (Class<? extends BaseAction> action : config.determineActions(StringUtils.join(config.workflows.keySet()))) {
            log.info("Instantiating constructor for {}", action.getSimpleName());
            BaseAction actionObject = action.getConstructor(WorkflowConfig.class).newInstance(config);
            if (actionObject instanceof BaseCommitAction) {
                ((BaseCommitAction) actionObject).setDraft(draft);
            }
            if (actionObject instanceof BaseMultiActionDataSupport) {
                ((BaseMultiActionDataSupport) actionObject).setMultiActionData(multiActionData);
            }
            if (runAllHelperMethods) {
                log.info("Running cannotRunAction method");
                actionObject.cannotRunAction();
                log.info("Running preprocess method");
                actionObject.preprocess();
            }
        }
    }

    private void dryRunActions(List<Class<? extends BaseAction>> actions) {
        log.info("Executing in dry run mode");
        log.info("Showing workflow actions that would have run for workflow argument [{}]", config.workflowsToRun);

        Padder actionsPadder = new Padder("Workflow Actions for workflow");
        actionsPadder.infoTitle();

        ConfigMappings configMappings = new ConfigMappings();
        Set<String> configOptions = new HashSet<>();
        for (Class<? extends BaseAction> action : actions) {
            ActionDescription description = action.getAnnotation(ActionDescription.class);
            if (description == null) {
                throw new RuntimeException("Please add a action description annotation for " + action.getSimpleName());
            }
            configOptions.addAll(configMappings.getConfigValuesForAction(action));
            String helpText = "- " + description.value();
            log.info(action.getSimpleName() + " " + helpText);
        }
        actionsPadder.infoTitle();

        Padder configPadder = new Padder("Config Options for workflow");
        configPadder.infoTitle();
        for (String configOption : configOptions) {
            Field matchingField = config.getMatchingField(configOption);
            if (matchingField == null) {
                log.info("{} - {}", configOption, "Unknown config option");
            } else {
                ConfigurableProperty matchingProperty = matchingField.getAnnotation(ConfigurableProperty.class);
                String matchingPropertyText = matchingProperty != null ? matchingProperty.help() : "Unknown config option";
                try {
                    Object matchingValue = matchingField.get(config);
                    String matchingValueText = convertObjectToString(matchingValue);
                    log.info("{}={} - {}", configOption, matchingValueText, matchingPropertyText);
                } catch (IllegalAccessException e) {
                    throw new RuntimeReflectiveOperationException(e);
                }
            }
        }
        configPadder.infoTitle();
    }

    private static String convertObjectToString(Object matchingValue) {
        String matchingValueText = "";
        if (matchingValue instanceof String[]) {
            matchingValueText = Arrays.toString((Object[]) matchingValue);
        } else if (matchingValue instanceof int[]) {
            matchingValueText = Arrays.toString((int[]) matchingValue);
        } else if (matchingValue != null) {
            matchingValueText = String.valueOf(matchingValue);
        }
        return matchingValueText;
    }

    private void runActions(List<Class<? extends BaseAction>> actions, WorkflowActionValues values) throws IllegalAccessException, URISyntaxException,
            InstantiationException, NoSuchMethodException, InvocationTargetException, IOException, ParseException {
        for (Class<? extends BaseAction> action : actions) {
            runAction(action, values);
        }
    }

    private void runAction(Class<? extends BaseAction> actionClass, WorkflowActionValues values) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, URISyntaxException, ParseException {
        BaseAction action = actionClass.getConstructor(WorkflowConfig.class).newInstance(config);
        log.debug("Executing workflow action {}", actionClass.getSimpleName());
        if (action instanceof BaseCommitAction) {
            ((BaseCommitAction) action).setDraft(values.getDraft());
        }
        if (action instanceof BaseMultiActionDataSupport) {
            ((BaseMultiActionDataSupport) action).setMultiActionData(values.getMultiActionData());
        }
        if (action instanceof BaseTrelloAction) {
            ((BaseTrelloAction) action).setSelectedBoard(values.getTrelloBoard());
        }

        String reasonForNotRunningAction = action.cannotRunAction();

        if (reasonForNotRunningAction == null) {
            action.preprocess();
            action.process();
        } else {
            log.info("Skipping running of action {} as {}.", action.getClass().getSimpleName(), reasonForNotRunningAction);
        }

        if (action instanceof BaseTrelloAction) {
            values.setTrelloBoard(((BaseTrelloAction) action).getSelectedBoard());
        }
    }
}
