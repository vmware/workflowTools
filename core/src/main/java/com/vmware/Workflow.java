package com.vmware;

import com.vmware.action.BaseAction;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.base.BaseIssuesProcessingAction;
import com.vmware.action.trello.BaseTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.JenkinsJobsConfig;
import com.vmware.config.UnknownWorkflowValueException;
import com.vmware.config.WorkflowActionValues;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowConfigParser;
import com.vmware.jira.domain.ProjectIssues;
import com.vmware.mapping.ConfigMappings;
import com.vmware.mapping.ConfigValuesCompleter;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.scm.Git;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.InvalidDataException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.input.CommaArgumentDelimeter;
import com.vmware.util.input.ImprovedArgumentCompleter;
import com.vmware.util.input.ImprovedStringsCompleter;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.Padder;
import jline.console.completer.ArgumentCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Main class for running the workflow application.
 */
public class Workflow {
    public static final List<String> BATCH_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("createTrelloBoardFromLabel", "closeOldReviews"));

    public static final List<String> GIT_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("commit", "commitAll", "amendCommit", "review", "pushable", "push", "submit")
    );

    public static final List<String> PERFORCE_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("moveOpenFilesToPendingChangelist", "review", "pushable", "submitChangelist")
    );

    private static final String EXIT_WORKFLOW = "exit";
    private final Git git = new Git();
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final WorkflowConfigParser configParser = new WorkflowConfigParser();
    private List<String> workflowHistory;
    private WorkflowConfig config;

    public void init(String[] args) {
        readWorkflowHistoryFile();

        config = configParser.parseWorkflowConfig(args);
        askForWorkflowIfEmpty();
    }

    private void updateWorkflowHistoryFile() {
        String argumentsText = configParser.getRuntimeArgumentsText();
        if (workflowHistory.isEmpty() || !workflowHistory.get(0).equals(argumentsText)) {
            workflowHistory.add(0, argumentsText);
        }
        if (workflowHistory.size() > 10) {
            workflowHistory.remove(10);
        }
        String userHome = System.getProperty("user.home");
        File workflowHistoryFile = new File(userHome + File.separator + ".workflowHistory.txt");
        IOUtils.write(workflowHistoryFile, workflowHistory);
    }

    private void readWorkflowHistoryFile() {
        String userHome = System.getProperty("user.home");
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
        String[] workflowTextPieces = splitWorkflowTextIntoArguments(workFlowText);
        configParser.updateWithRuntimeArguments(config, workflowTextPieces);
    }

    private String[] splitWorkflowTextIntoArguments(String workFlowText) {
        String[] quotedTextPieces = workFlowText.split("[\"']");
        List<String> workflowTextPieces = new ArrayList<>();
        for (int i = 0; i < quotedTextPieces.length; i ++) {
            if (i % 2 == 0) {
                String[] piecesSplitBySpace = quotedTextPieces[i].split(" ");
                if (!quotedTextPieces[i].endsWith(" ") && i < quotedTextPieces.length - 1) { // reattach piece of text after space and before quote
                    quotedTextPieces[i+1] = piecesSplitBySpace[piecesSplitBySpace.length - 1] + quotedTextPieces[i+1];
                    if (piecesSplitBySpace.length > 1) {
                        workflowTextPieces.addAll(Arrays.asList(Arrays.copyOf(piecesSplitBySpace, piecesSplitBySpace.length - 1)));
                    }
                } else {
                    workflowTextPieces.addAll(Arrays.asList(piecesSplitBySpace));
                }
            } else {
                workflowTextPieces.add(quotedTextPieces[i]);
            }
        }
        return workflowTextPieces.toArray(new String[workflowTextPieces.size()]);
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

    public void runWorkflow() {
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
                Date startingDate = new Date();
                runActions(workflowActions, new WorkflowActionValues());
                outputTotalExecutionTime(startingDate);
            }
        } catch (UnknownWorkflowValueException e) {
            log.error(e.getMessage());
            askForWorkflow();
            runWorkflow();
        } catch (InvalidDataException iie) {
            if (log.isDebugEnabled()) {
                throw iie;
            } else {
                log.error(iie.getMessage());
            }
        }
    }

    private void outputTotalExecutionTime(Date startingDate) {
        if (log.isDebugEnabled()) {
            log.info("");
            long totalElapsedTime = new Date().getTime() - startingDate.getTime();
            log.debug("Workflow execution time {} ms", totalElapsedTime);
        }
    }

    private void checkAllActionsCanBeInstantiated(boolean runAllHelperMethods) {
        log.info("Checking that each action value in the workflows is valid");
        ReviewRequestDraft draft = new ReviewRequestDraft();
        ProjectIssues projectIssues = new ProjectIssues();
        for (Class<? extends BaseAction> action : config.determineActions(StringUtils.join(config.workflows.keySet()))) {
            log.info("Instantiating constructor for {}", action.getSimpleName());
            BaseAction actionObject = instantiateAction(action);

            if (actionObject instanceof BaseCommitAction) {
                ((BaseCommitAction) actionObject).setDraft(draft);
            }
            if (actionObject instanceof BaseIssuesProcessingAction) {
                ((BaseIssuesProcessingAction) actionObject).setProjectIssues(projectIssues);
            }
            if (runAllHelperMethods) {
                log.info("Running asyncSetup");
                actionObject.asyncSetup();
                log.info("Running failWorkflowIfConditionNotMet");
                actionObject.failWorkflowIfConditionNotMet();
                log.info("Running cannotRunAction method");
                actionObject.cannotRunAction();
                log.info("Running preprocess method");
                actionObject.preprocess();
            } else {
                log.info("Running asyncSetup");
                actionObject.asyncSetup();
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
        if (git.isGitInstalled()) {
            log.info("Config values can also be set by executing git config [name in square brackets] [configValue]");
        }
        for (String configOption : configOptions) {
            Field matchingField = config.getMatchingField(configOption);
            if (matchingField == null) {
                log.info("{} - {}", configOption, "Unknown config option");
            } else {
                ConfigurableProperty matchingProperty = matchingField.getAnnotation(ConfigurableProperty.class);
                String matchingPropertyText = matchingProperty != null ? matchingProperty.help() : "Unknown config option";
                String matchingValueText;
                if (configOption.equals("--jenkins-jobs")) {
                    JenkinsJobsConfig jobsConfig = config.getJenkinsJobsConfig();
                    matchingValueText = jobsConfig.toString();
                } else {
                    Object matchingValue = ReflectionUtils.getValue(matchingField, config);
                    matchingValueText = StringUtils.convertObjectToString(matchingValue);
                }
                String source = config.getFieldValueSource(matchingField.getName());
                String gitConfigValue = "";
                if (git.isGitInstalled()) {
                    gitConfigValue = "[" + matchingField.getName() + "]";
                }
                log.info("{}{}={} source=[{}] - {}", configOption, gitConfigValue, matchingValueText, source, matchingPropertyText);
            }
        }
        configPadder.infoTitle();
    }

    private void runActions(List<Class<? extends BaseAction>> actions, WorkflowActionValues values) {
        List<BaseAction> actionsToRun = new ArrayList<>();
        for (Class<? extends BaseAction> action : actions) {
            actionsToRun.add(instantiateAction(action));
        }


        ConcurrentLinkedQueue<BaseAction> actionsSetup = new ConcurrentLinkedQueue<>();
        SetupActions setupActions = new SetupActions(actionsToRun, actionsSetup);
        new Thread(setupActions).start();
        ThreadUtils.sleep(10, TimeUnit.MILLISECONDS);

        int waitTimeInMilliSeconds = 0;
        Map<String, Long> executionTimesPerAction = new LinkedHashMap<>();
        for (BaseAction action : actionsToRun) {
            while (!actionsSetup.contains(action)) {
                String actionName = action.getClass().getSimpleName();
                if (waitTimeInMilliSeconds > 10000) {
                    throw new RuntimeException(actionName + ".asyncSetup failed to finish in 10 seconds");
                }
                if (waitTimeInMilliSeconds > 0 && waitTimeInMilliSeconds % 1000 == 0) {
                    log.debug("Waiting for {}.asyncSetup to finish, waited {} seconds",
                            actionName, TimeUnit.MILLISECONDS.toSeconds(waitTimeInMilliSeconds));
                }
                ThreadUtils.sleep(50, TimeUnit.MILLISECONDS);
                waitTimeInMilliSeconds += 50;
            }
            Date startingDate = new Date();
            runAction(action, values);
            long elapsedTime = new Date().getTime() - startingDate.getTime();
            executionTimesPerAction.put(action.getClass().getSimpleName(), elapsedTime);
        }
        outputExecutionTimes(executionTimesPerAction);
    }

    private void outputExecutionTimes(Map<String, Long> executionTimesPerAction) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.info("");
        log.debug("Execution time per action");
        for (String actionName : executionTimesPerAction.keySet()) {
            log.debug("{} - {} ms", actionName, executionTimesPerAction.get(actionName));
        }
    }


    private void runAction(BaseAction action, WorkflowActionValues values) {
        String actionName = action.getClass().getSimpleName();
        log.debug("Executing workflow action {}", actionName);
        setWorkflowValuesOnAction(action, values);

        String reasonForFailingAction = action.failWorkflowIfConditionNotMet();

        if (reasonForFailingAction != null) {
            log.error("Workflow failed by action {} as {}", actionName, reasonForFailingAction);
            System.exit(1);
        }

        String reasonForNotRunningAction = action.cannotRunAction();

        if (reasonForNotRunningAction != null) {
            log.info("Skipping running of action {} as {}", actionName, reasonForNotRunningAction);
        } else {
            action.preprocess();
            action.process();
        }

        if (action instanceof BaseTrelloAction) {
            values.setTrelloBoard(((BaseTrelloAction) action).getSelectedBoard());
        }
    }

    private void setWorkflowValuesOnAction(BaseAction action, WorkflowActionValues values) {
        if (action instanceof BaseCommitAction) {
            ((BaseCommitAction) action).setDraft(values.getDraft());
        }
        if (action instanceof BaseIssuesProcessingAction) {
            ((BaseIssuesProcessingAction) action).setProjectIssues(values.getProjectIssues());
        }
        if (action instanceof BaseTrelloAction) {
            ((BaseTrelloAction) action).setSelectedBoard(values.getTrelloBoard());
        }
    }

    private BaseAction instantiateAction(Class<? extends BaseAction> actionToInstantiate) {
        try {
            return actionToInstantiate.getConstructor(WorkflowConfig.class).newInstance(config);
        } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    private class SetupActions implements Runnable {

        private ConcurrentLinkedQueue<BaseAction> actionsRun;
        private List<BaseAction> actionsToRun;

        SetupActions(List<BaseAction> actionsToRun, ConcurrentLinkedQueue<BaseAction> actionsRun) {
            this.actionsToRun = actionsToRun;
            this.actionsRun = actionsRun;
        }

        @Override
        public void run() {
            for (BaseAction action : actionsToRun) {
                log.debug("Preprocessing {}", action.getClass().getSimpleName());
                try {
                    action.asyncSetup();
                } catch (Exception e) {
                    log.error(e.getMessage() != null ? e.getMessage() : "", e);
                    e.printStackTrace(); // log.error doesn't seem to include the stack trace
                    System.exit(1);
                }
                actionsRun.add(action);
            }
            log.debug("Preprocessing finished");
        }
    }
}
