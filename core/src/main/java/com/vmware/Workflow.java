package com.vmware;

import java.io.File;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.vmware.action.info.DisplayLineBreak;
import com.vmware.action.info.DisplayInfo;
import com.vmware.action.info.GenerateAutoCompleteValues;
import com.vmware.config.ActionDescription;
import com.vmware.config.CalculatedProperty;
import com.vmware.config.ConfigurableProperty;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.UnknownWorkflowValueException;
import com.vmware.config.WorkflowAction;
import com.vmware.config.WorkflowActionValues;
import com.vmware.config.WorkflowActions;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowConfigParser;
import com.vmware.config.WorkflowField;
import com.vmware.config.WorkflowFields;
import com.vmware.config.WorkflowParameter;
import com.vmware.mapping.ConfigMappings;
import com.vmware.mapping.ConfigValuesCompleter;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.CancelException;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.SkipActionException;
import com.vmware.util.input.CommaArgumentDelimeter;
import com.vmware.util.input.ImprovedArgumentCompleter;
import com.vmware.util.input.ImprovedStringsCompleter;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.logging.Padder;
import com.vmware.util.scm.Git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jline.console.completer.ArgumentCompleter;

/**
 * Main class for running the workflow application.
 */
public class Workflow {
    public static final List<String> BATCH_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("createTrelloBoardFromLabel", "createTrelloBoardFromFixVersion", "setStoryPointsForBoard", "closeOldReviews"));

    public static final List<String> GIT_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("commit", "commitAll", "amendCommit", "amendCommitAll", "review", "pushable", "push", "merge")
    );

    public static final List<String> PERFORCE_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("moveOpenFilesToPendingChangelist", "review", "pushable", "submit"));

    public static final List<String> VAPP_MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("deleteVapp", "renameVapp", "updateVappLease", "tailVappLogFile", "displayVappJson", "openVappProvider")
    );

    private static final String QUIT_WORKFLOW = "q";
    private final Git git = new Git();
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final WorkflowConfigParser configParser = new WorkflowConfigParser();
    private List<String> workflowHistory;
    private WorkflowConfig config;
    private ServiceLocator serviceLocator;
    private boolean firstTime = true;
    private boolean displayedShellInfoMessage = false;
    private String username = null;
    private final List<String> args;

    public Workflow(ClassLoader appClassLoader, List<String> args) {
        WorkflowConfig.appClassLoader = appClassLoader;
        this.args = args;
    }

    private void init() {
        readWorkflowHistoryFile();
        config = configParser.parseWorkflowConfig(username, args);
        username = config.username;
        serviceLocator = new ServiceLocator(config);
        askForWorkflowIfEmpty(firstTime);
        firstTime = false;
    }

    private void updateWorkflowHistoryFile() {
        String argumentsText = configParser.getRuntimeArgumentsText();
        if (argumentsText != null && argumentsText.startsWith(GenerateAutoCompleteValues.class.getSimpleName())) {
            log.info("Not adding action {} to history as it is just used for auto completion support", GenerateAutoCompleteValues.class.getSimpleName());
            return;
        }
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


    private void askForWorkflowIfEmpty(boolean firstTime) {
        if (StringUtils.isNotBlank(config.workflowsToRun)) {
            displayShellInfoMessageIfNeeded();
            return;
        }

        askForWorkflow(firstTime);
    }

    private void askForWorkflow(boolean firstTime) {
        displayShellInfoMessageIfNeeded();
        if (firstTime) {
            log.info("Press ENTER for getting started info");
            if (StringUtils.isNotBlank(config.projectDocumentationUrl)) {
                log.info("Project specific documentation can be found here: {}", config.projectDocumentationUrl);
            }

            log.info("");

            log.info("Press tab to see a list of available workflows, up to see previously entered workflows");
        }

        ArgumentCompleter argumentsCompleter = createWorkflowCompleter();

        String workFlowText = InputUtils.readValue("Workflow(type " + QUIT_WORKFLOW + " to quit)",
                argumentsCompleter, workflowHistory).trim();
        if (workFlowText.equals(QUIT_WORKFLOW) || workFlowText.equalsIgnoreCase("exit")) {
            log.debug("Quitting");
            System.exit(0);
        }
        List<String> workflowTextPieces = splitWorkflowTextIntoArguments(workFlowText);
        configParser.updateWithRuntimeArguments(config, workflowTextPieces);
    }

    private void displayShellInfoMessageIfNeeded() {
        if (!displayedShellInfoMessage && config.shellMode) {
            log.info("Running in shell mode");
            displayedShellInfoMessage = true;
        }
    }

    private List<String> splitWorkflowTextIntoArguments(String workFlowText) {
        String[] quotedTextPieces = workFlowText.split("[\"]");
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
        return workflowTextPieces;
    }

    private ArgumentCompleter createWorkflowCompleter() {
        List<String> autocompleteList = new ArrayList<>();

        for (String workflow : config.workflows.keySet()) {
            if (config.supportingWorkflows.contains(workflow)) {
                // ! means that it won't show up if nothing is entered
                autocompleteList.add("!" + workflow);
            } else {
                autocompleteList.add(workflow);
            }
        }
        WorkflowActions workflowActions = new WorkflowActions(config, WorkflowConfig.appClassLoader);
        // ! means that it won't show up if nothing is entered
        autocompleteList.addAll(workflowActions.getWorkflowActionClasses()
                .stream().map(workflowAction -> "!" + workflowAction.getSimpleName()).collect(Collectors.toList()));
        ImprovedStringsCompleter workflowCompleter = new ImprovedStringsCompleter(autocompleteList);
        workflowCompleter.setDelimeterText("");
        workflowCompleter.addValue("!" + QUIT_WORKFLOW);
        ArgumentCompleter workflowArgumentCompleter = new ImprovedArgumentCompleter(new CommaArgumentDelimeter(), workflowCompleter);
        workflowArgumentCompleter.setStrict(false);

        ConfigValuesCompleter configValueCompleter = new ConfigValuesCompleter(config);
        configValueCompleter.setDelimeterText("");
        ArgumentCompleter argumentsCompleter = new ImprovedArgumentCompleter(workflowArgumentCompleter, configValueCompleter);
        argumentsCompleter.setStrict(false);
        return argumentsCompleter;
    }

    public void runWorkflow() {
        try {
            init();
            if (StringUtils.isEmpty(config.workflowsToRun) && config.shellMode) {
                askForWorkflow(false);
                runWorkflow();
            } else if (StringUtils.isEmpty(config.workflowsToRun)) {
                // default workflow
                config.workflowsToRun = "intro";
            }

            String workflowToRun = config.workflowsToRun;
            if (Arrays.asList("abalta", "anabalta").contains(workflowToRun)) {
                checkAllActionsCanBeInstantiated(workflowToRun.equals("anabalta"));
                return;
            }

            WorkflowActions workflowActions = new WorkflowActions(config, WorkflowConfig.appClassLoader);
            List<WorkflowAction> actions = workflowActions.determineActions(workflowToRun);
            // update history file after all the workflow has been determined to be valid
            updateWorkflowHistoryFile();
            if (config.dryRun) {
                dryRunActions(actions);
            } else {
                Date startingDate = new Date();
                runActions(actions, new WorkflowActionValues());
                outputTotalExecutionTime(startingDate);
            }
            runWorkflowAgain();
        } catch (UnknownWorkflowValueException e) {
            log.error(e.getMessage());
            askForWorkflow(true);
            runWorkflowAgain();
        } catch (CancelException ee) {
            log.info("");
            if (!ee.getLogLevel().isDebug() && StringUtils.isNotEmpty(config.errorMessageForCancel)) {
                new DynamicLogger(log).log(ee.getLogLevel(), "Canceling as " + config.errorMessageForCancel);
            } else {
                new DynamicLogger(log).log(ee.getLogLevel(), "Canceling as " + ee.getMessage());
            }
            runWorkflowAgain();
        } catch (FatalException iie) {
            log.info("");
            if (log.isDebugEnabled()) {
                throw iie;
            } else {
                log.error(iie.getMessage());
                runWorkflowAgain();
            }
        }
    }

    private void runWorkflowAgain() {
        if (config != null && config.shellMode) {
            runWorkflow();
        }
    }

    private void outputTotalExecutionTime(Date startingDate) {
        if (log.isDebugEnabled()) {
            log.info("");
            long totalElapsedTimeInMs = System.currentTimeMillis() - startingDate.getTime();
            if (totalElapsedTimeInMs < 10) {
                log.trace("Workflow execution time {} seconds", TimeUnit.MILLISECONDS.toSeconds(totalElapsedTimeInMs));
            } else {
                log.debug("Workflow execution time {} seconds", TimeUnit.MILLISECONDS.toSeconds(totalElapsedTimeInMs));
            }

        }
    }

    private void checkAllActionsCanBeInstantiated(boolean runAllHelperMethods) {
        log.info("Checking that each action value in the workflows is valid");
        WorkflowActions workflowActions = new WorkflowActions(config, WorkflowConfig.appClassLoader);
        List<WorkflowAction> actions =
                workflowActions.determineActions(StringUtils.join(config.workflows.keySet()));
        WorkflowActionValues actionValues = new WorkflowActionValues();
        for (WorkflowAction action : actions) {
            log.info("Instantiating constructor for {}", action.getActionClassName());
            action.instantiateAction(config, serviceLocator);

            action.setWorkflowValuesOnAction(actionValues);
            try {
                if (runAllHelperMethods) {
                    log.info("Running asyncSetup");
                    action.asyncSetup();
                    log.info("Running checkIfWorkflowShouldBeFailed");
                    action.checkIfWorkflowShouldBeFailed();
                    log.info("Running checkIfActionShouldBeSkipped method");
                    action.checkIfActionShouldBeSkipped();
                    log.info("Running preprocess method");
                    action.preprocess();
                } else {
                    log.info("Running asyncSetup");
                    action.asyncSetup();
                }
            } catch (SkipActionException sae) {
                log.info("Skipping {} as {}", action.getActionClassName(), sae.getMessage());
            } catch (CancelException ce) {
                log.info("Canceling {} as {}", action.getActionClassName(), ce.getMessage());
            }

        }
    }

    private void dryRunActions(List<WorkflowAction> actions) {
        log.info("Executing in dry run mode");
        log.info("Showing workflow actions that would have run for workflow argument [{}]", config.workflowsToRun);

        Padder actionsPadder = new Padder("Workflow Actions");
        actionsPadder.infoTitle();

        ConfigMappings configMappings = new ConfigMappings();
        Set<String> configOptions = new HashSet<>();
        Set<String> configValuesToRemove = new HashSet<>();
        Padder sectionPadder = null;
        for (WorkflowAction action : actions) {
            ActionDescription description = action.getActionDescription();
            if (description == null) {
                throw new RuntimeException("Please add a action description annotation for " + action.getActionClassName());
            }
            if (StringUtils.isNotEmpty(action.getSectionName()) && (sectionPadder == null || !sectionPadder.getTitle().equals(action.getSectionName()))) {
                if (sectionPadder != null){
                    sectionPadder.infoTitle();
                }
                sectionPadder = new Padder(action.getSectionName());
                sectionPadder.infoTitle();
            } else if (StringUtils.isEmpty(action.getSectionName()) && sectionPadder != null) {
                sectionPadder.infoTitle();
                sectionPadder = null;
            }
            configOptions.addAll(configMappings.getAutoCompleteValuesForAction(action));
            configValuesToRemove.addAll(action.configFlagsToAlwaysRemoveFromCompleter());
            config.addGeneratedVariables();
            List<WorkflowParameter> params = action.getOverriddenConfigValues();
            if (action.getActionClassName().equals(DisplayLineBreak.class.getSimpleName())) {
                log.info("");
            } else if (action.getActionClassName().equals(DisplayInfo.class.getSimpleName())) {
                log.info(params.get(0).getValue());
                log.info("");
            } else {
                log.info(action.getActionClassName() + " - " + description.value());
                for (WorkflowParameter parameter : params) {
                    log.info("{}   {}={}", StringUtils.repeat(action.getActionClassName().length(), " "),
                            parameter.getName(), parameter.getValue());
                }
            }
        }
        if (sectionPadder != null) {
            sectionPadder.infoTitle();
        }
        actionsPadder.infoTitle();

        Padder configPadder = new Padder("Config Options");
        configPadder.infoTitle();
        if (git.isGitInstalled()) {
            log.info("Config values can also be set by executing git config [name in square brackets] [configValue]");
        }

        configOptions.removeAll(configValuesToRemove);
        if (config.replacementVariables.hasVariable(ReplacementVariables.VariableName.REPO_DIR)) {
            configOptions.remove("--changelist-id");
        }

        WorkflowFields configurableFields = config.getConfigurableFields();
        List<String> sortedConfigOptions = configOptions.stream().sorted().collect(Collectors.toList());
        int counter = 0;
        for (String configOption : sortedConfigOptions) {
            WorkflowField matchingField = configurableFields.getMatchingField(configOption);
            if (matchingField == null) {
                log.info("{} - {}", configOption, "Unknown config option");
            } else {
                ConfigurableProperty matchingProperty = matchingField.configAnnotation();
                String matchingPropertyText = matchingProperty != null ? matchingProperty.help() : "Unknown config option";
                String matchingValueText;
                String source = null;
                if (configOption.equals("--jenkins-jobs")) {
                    matchingValueText = config.getJenkinsJobsConfig().toString();
                } else {
                    CalculatedProperty property = config.valueForField(matchingField);
                    matchingValueText = StringUtils.convertObjectToString(property.getValue());
                    if (property.getSource() != null && !property.getSource().equals(matchingField.getName())) {
                        source = property.getSource();
                    }
                }
                if (source == null) {
                    source = configurableFields.getFieldValueSource(matchingField.getName());
                }
                String gitConfigValue = "";
                if (git.isGitInstalled()) {
                    gitConfigValue = "[" + matchingField.getName() + "]";
                }
                if (counter++ >= 5 && counter++ % 5 == 1) {
                    log.info("");
                }
                log.info("{}{}={} source=[{}] - {}", configOption, gitConfigValue, matchingValueText, source, matchingPropertyText);
            }
        }
        configPadder.infoTitle();

        Map<String, String> replacementVariables = config.replacementVariables.values();
        if (replacementVariables.isEmpty() || (replacementVariables.size() == 1
                && replacementVariables.keySet().iterator().next().equals(ReplacementVariables.VariableName.REPO_DIR.name()))) {
            return;
        }
        Padder variablePadder = new Padder("Variables for workflow");
        variablePadder.infoTitle();
        for (String key : replacementVariables.keySet().stream().sorted().collect(Collectors.toList())) {
            log.info("{}{}={}", ReplacementVariables.CONFIG_PREFIX, key, replacementVariables.get(key));
        }
        variablePadder.infoTitle();
    }

    private void runActions(List<WorkflowAction> actions, WorkflowActionValues values) {
        actions.forEach(action -> action.instantiateAction(config, serviceLocator));


        ConcurrentLinkedQueue<WorkflowAction> actionsSetup = new ConcurrentLinkedQueue<>();
        SetupActions setupActions = new SetupActions(actions, actionsSetup);
        new Thread(setupActions).start();
        ThreadUtils.sleep(10, TimeUnit.MILLISECONDS);

        AtomicInteger waitTimeInMilliSeconds = new AtomicInteger(0);
        Map<String, Long> executionTimesPerAction = new LinkedHashMap<>();
        boolean alwaysProceed = false;
        for (WorkflowAction action : actions) {
            if (config.checkPoint && !alwaysProceed) {
                String skipResponse = checkWhetherToSkipAction(action);
                alwaysProceed = StringUtils.isNotBlank(skipResponse) && "always".startsWith(skipResponse);
                boolean skippable = StringUtils.isNotBlank(skipResponse) && "skip".startsWith(skipResponse);
                if (skippable && !alwaysProceed) {
                    log.info("Skipping action {}", action.getActionClassName());
                    continue;
                }
            }
            waitForAsyncActionSetupToFinish(actionsSetup, waitTimeInMilliSeconds, action);
            Date startingDate = new Date();
            runAction(action, values);
            long elapsedTime = System.currentTimeMillis() - startingDate.getTime();
            executionTimesPerAction.put(action.getClass().getSimpleName(), elapsedTime);
        }
        outputExecutionTimes(executionTimesPerAction);
    }

    private void waitForAsyncActionSetupToFinish(ConcurrentLinkedQueue<WorkflowAction> actionsSetup, AtomicInteger waitTimeInMilliSeconds, WorkflowAction action) {
        while (!actionsSetup.contains(action)) {
            String actionName = action.getActionClassName();
            int waitTimeValue = waitTimeInMilliSeconds.get();
            if (waitTimeValue > 30000) {
                log.error(actionName + ".asyncSetup failed to finish in 30 seconds");
                System.exit(1);
            }
            if (waitTimeValue > 0 && waitTimeValue % 1000 == 0) {
                log.debug("Waiting for {}.asyncSetup to finish, waited {} seconds",
                        actionName, TimeUnit.MILLISECONDS.toSeconds(waitTimeValue));
            }
            ThreadUtils.sleep(50, TimeUnit.MILLISECONDS);
            waitTimeInMilliSeconds.addAndGet(50);
        }
    }

    private String checkWhetherToSkipAction(WorkflowAction action) {
        log.info("Next action is {}", action.getActionClassName());
        String confirmation = InputUtils.readValue("(s)kip (p)roceed (a)lways proceed (e)xit", "yes","no");
        if (StringUtils.isNotBlank(confirmation) && "exit".startsWith(confirmation)) {
            throw new CancelException(LogLevel.INFO, "exit selected before running " + action.getActionClassName());
        }
        return confirmation;
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


    private void runAction(WorkflowAction action, WorkflowActionValues values) {
        String actionName = action.getActionClassName();
        log.debug("Executing workflow action {}", actionName);
        action.setWorkflowValuesOnAction(values);
        try {
            action.checkIfActionShouldBeSkipped();
            action.checkIfWorkflowShouldBeFailed();
            action.preprocess();
            action.process();
        } catch (SkipActionException cbre) {
            log.info("Skipping running of action {} as {}", actionName, cbre.getMessage());
        }

        action.updateWorkflowValues(values);
    }

    private class SetupActions implements Runnable {

        private ConcurrentLinkedQueue<WorkflowAction> actionsRun;
        private List<WorkflowAction> actionsToRun;

        SetupActions(List<WorkflowAction> actionsToRun, ConcurrentLinkedQueue<WorkflowAction> actionsRun) {
            this.actionsToRun = actionsToRun;
            this.actionsRun = actionsRun;
        }

        @Override
        public void run() {
            for (WorkflowAction action : actionsToRun) {
                log.debug("Preprocessing {}", action.getActionClassName());
                try {
                    action.asyncSetup();
                } catch (Exception e) {
                    log.error("{}\n{}", e.getMessage(), StringUtils.exceptionAsString(e));
                    System.exit(1);
                }
                actionsRun.add(action);
            }
            log.debug("Preprocessing finished");
        }
    }
}
