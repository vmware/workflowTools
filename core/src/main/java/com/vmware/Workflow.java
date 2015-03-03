package com.vmware;

import com.vmware.action.AbstractAction;
import com.vmware.action.base.AbstractBatchIssuesAction;
import com.vmware.action.base.AbstractCommitAction;
import com.vmware.action.trello.AbstractTrelloAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.CommandLineArgumentsParser;
import com.vmware.config.LogLevel;
import com.vmware.config.UnknownWorkflowValueException;
import com.vmware.config.WorkflowActionValues;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.ProjectIssues;
import com.vmware.mapping.ConfigMappings;
import com.vmware.mapping.ConfigValuesCompleter;
import com.vmware.rest.SslUtils;
import com.vmware.rest.json.ConfiguredGsonBuilder;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.ClasspathResource;
import com.vmware.utils.input.CommaArgumentDelimeter;
import com.vmware.utils.IOUtils;
import com.vmware.utils.input.ImprovedArgumentCompleter;
import com.vmware.utils.input.ImprovedStringsCompleter;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.Padder;
import com.vmware.utils.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jline.console.completer.ArgumentCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;

/**
 * Main class for running the workflow application.
 */
public class Workflow {
    public static final List<String> MAIN_WORKFLOWS = Collections.unmodifiableList(
            Arrays.asList("commit", "review", "pushable", "pushIt", "createTrelloBoardFromLabel"
                    , "commitAll", "amendCommit", "commitOffline", "closeOldReviews", "restartJobs", "review"));

    private static final String EXIT_WORKFLOW = "exit";

    private static final Logger log = LoggerFactory.getLogger(Workflow.class.getName());
    private static final Git git = new Git();
    private static final CommandLineArgumentsParser argsParser = new CommandLineArgumentsParser();
    private static Gson gson;
    private static List<String> workflowHistory;

    public static void main(String[] args) throws Exception {
        LogManager.getLogManager().reset();
        gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();

        readWorkflowHistoryFile();
        argsParser.generateArgumentMap(args);
        WorkflowConfig workflowConfig = parseWorkflowConfig(argsParser);

        askForWorkflowIfEmpty(workflowConfig);
        try {
            runWorkflow(workflowConfig);
        } catch (IllegalArgumentException iae) {
            log.error(iae.getMessage());
            if (log.isDebugEnabled()) {
                iae.printStackTrace();
            }
        }
    }

    private static void updateWorkflowHistoryFile() throws IOException {
        String argumentsText = argsParser.getArgumentsText();
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

    private static void readWorkflowHistoryFile() throws IOException {
        String userHome = System.getProperty( "user.home" );
        File workflowHistoryFile = new File(userHome + File.separator + ".workflowHistory.txt");
        if (!workflowHistoryFile.exists()) {
            workflowHistory = new ArrayList<String>();
        } else {
            workflowHistory = IOUtils.readLines(new FileInputStream(workflowHistoryFile));
        }
    }

    private static ConsoleHandler createHandler() {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleLogFormatter());
        handler.setLevel(Level.FINEST);
        return handler;
    }

    private static void askForWorkflowIfEmpty(WorkflowConfig config) throws IOException, IllegalAccessException {
        if (StringUtils.isNotBlank(config.workflowsToRun)) {
            return;
        }

        log.info("No workflow entered. Please enter workflow");
        askForWorkflow(config);
    }

    private static void askForWorkflow(WorkflowConfig config) throws IOException, IllegalAccessException {
        log.info("Press tab to see a list of available workflows");
        log.info("Press up to see previously entered workflows");
        log.info("Type {} to exit without running a workflow", EXIT_WORKFLOW);

        ArgumentCompleter argumentsCompleter = createWorkflowCompleter(config);

        String workFlowText = InputUtils.readValue("Workflow (press ENTER for getting started info)",
                argumentsCompleter, workflowHistory);
        if (workFlowText.trim().equals(EXIT_WORKFLOW)) {
            log.info("Exiting");
            System.exit(0);
        }
        argsParser.generateArgumentMap(workFlowText.split(" "));
        applyRuntimeArguments(config);
        setLogLevel(config);
    }

    private static ArgumentCompleter createWorkflowCompleter(WorkflowConfig config) {
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

    private static void runWorkflow(WorkflowConfig workflowConfig) throws ClassNotFoundException, IllegalAccessException,
            URISyntaxException, InstantiationException, NoSuchMethodException, InvocationTargetException,
            IOException, ParseException, UnknownWorkflowValueException {

        if (workflowConfig.disableSslCertValidation) {
            log.info("SSL Certificate validation is disabled");
            SslUtils.trustAllHttpsCertificates();
        }

        if (StringUtils.isBlank(workflowConfig.workflowsToRun)) {
            // default workflow
            workflowConfig.workflowsToRun = "intro";
        }

        String workflowToRun = workflowConfig.workflowsToRun;
        if (workflowToRun.equals("abalta") || workflowToRun.equals("anabalta")) {
            checkAllActionsCanBeInstantiated(workflowConfig, workflowToRun.equals("anabalta"));
            return;
        }

        try {
            List<Class<? extends AbstractAction>> workflowActions = workflowConfig.determineActions(workflowToRun);
            // update history file after all the workflow has been determined to be valid
            updateWorkflowHistoryFile();
            log.trace("Workflow Config\n{}", gson.toJson(workflowConfig));
            if (workflowConfig.dryRun) {
                dryRunActions(workflowActions, workflowConfig);
            } else {
                runActions(workflowActions, workflowConfig, new WorkflowActionValues());
            }
        } catch (UnknownWorkflowValueException e) {
            log.error(e.getMessage());
            askForWorkflow(workflowConfig);
            runWorkflow(workflowConfig);
        }
    }

    private static void checkAllActionsCanBeInstantiated(WorkflowConfig workflowConfig, boolean runAllHelperMethods)
            throws ClassNotFoundException, IllegalAccessException, UnknownWorkflowValueException, InstantiationException,
            InvocationTargetException, NoSuchMethodException, IOException, URISyntaxException {
        log.trace("Workflow Config\n{}", gson.toJson(workflowConfig));
        log.info("Checking that each action value in the workflows is valid");
        ReviewRequestDraft draft = new ReviewRequestDraft();
        ProjectIssues projectIssues = new ProjectIssues();
        for (Class<? extends AbstractAction> action : workflowConfig.determineActions(StringUtils.join(workflowConfig.workflows.keySet()))) {
            log.info("Instantiating constructor for {}", action.getSimpleName());
            AbstractAction actionObject = action.getConstructor(WorkflowConfig.class).newInstance(workflowConfig);
            if (actionObject instanceof AbstractCommitAction) {
                ((AbstractCommitAction) actionObject).setDraft(draft);
            }
            if (actionObject instanceof AbstractBatchIssuesAction) {
                ((AbstractBatchIssuesAction) actionObject).setProjectIssues(projectIssues);
            }
            if (runAllHelperMethods) {
                log.info("Running canRunAction method");
                actionObject.canRunAction();
                log.info("Running preprocess method");
                actionObject.preprocess();
            }
        }
    }

    private static void dryRunActions(List<Class<? extends AbstractAction>> actions, WorkflowConfig config) {
        log.info("Executing in dry run mode");
        log.info("Showing workflow actions that would have run for workflow argument [{}]", config.workflowsToRun);

        Padder actionsPadder = new Padder("Workflow Actions");
        actionsPadder.infoTitle();

        ConfigMappings configMappings = new ConfigMappings();
        Set<String> configOptions = new HashSet<String>();
        for (Class<? extends AbstractAction> action : actions) {
            ActionDescription description = action.getAnnotation(ActionDescription.class);
            if (description == null) {
                throw new RuntimeException("Please add a action description annotation for " + action.getSimpleName());
            }
            configOptions.addAll(configMappings.getConfigValuesForAction(action));
            String helpText = "- " + description.value();
            log.info(action.getSimpleName() + " " + helpText);
        }
        actionsPadder.infoTitle();

        Padder configPadder = new Padder("Config Options");
        configPadder.infoTitle();
        for (String configOption : configOptions) {
            log.info("{} - {}", configOption, config.getMatchingProperty(configOption).help());
        }
        configPadder.infoTitle();
    }

    private static void runActions(List<Class<? extends AbstractAction>> actions, WorkflowConfig config, WorkflowActionValues values) throws IllegalAccessException, URISyntaxException,
            InstantiationException, NoSuchMethodException, InvocationTargetException, IOException, ParseException {
        for (Class<? extends AbstractAction> action : actions) {
            runAction(config, action, values);
        }
    }

    private static void runAction(WorkflowConfig config, Class<? extends AbstractAction> actionClass, WorkflowActionValues values) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, IOException, URISyntaxException, ParseException {
        AbstractAction action = actionClass.getConstructor(WorkflowConfig.class).newInstance(config);
        log.debug("Executing workflow action {}", actionClass.getSimpleName());
        if (action instanceof AbstractCommitAction) {
            ((AbstractCommitAction) action).setDraft(values.getDraft());
        }
        if (action instanceof AbstractBatchIssuesAction) {
            ((AbstractBatchIssuesAction) action).setProjectIssues(values.getProjectIssues());
        }
        if (action instanceof AbstractTrelloAction) {
            ((AbstractTrelloAction) action).setSelectedBoard(values.getTrelloBoard());
        }

        boolean canRunAction = action.canRunAction();

        if (canRunAction) {
            action.preprocess();
            action.process();
        }

        if (action instanceof AbstractTrelloAction) {
            values.setTrelloBoard(((AbstractTrelloAction) action).getSelectedBoard());
        }
    }

    private static WorkflowConfig parseWorkflowConfig(CommandLineArgumentsParser argsParser) throws IOException, IllegalAccessException {
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        globalLogger.addHandler(createHandler());

        WorkflowConfig internalConfig = readInternalConfig();
        internalConfig.generateConfigurablePropertyList();

        List<String> loadedConfigFiles = new ArrayList<String>();

        // apply twice so that setting a debug log level can be detected earlier
        applyRuntimeArguments(internalConfig);
        setLogLevel(internalConfig);

        internalConfig.setGitOriginUrlAsReviewBoardRepo();

        applyRepoConfigFileIfExists(internalConfig, loadedConfigFiles);

        applyUserConfigFileIfExists(internalConfig, loadedConfigFiles);

        internalConfig.applyGitConfigValues();

        applySpecifiedConfigFiles(argsParser, internalConfig, loadedConfigFiles);

        applyRuntimeArguments(internalConfig);

        setLogLevel(internalConfig);

        internalConfig.loadedConfigFiles = loadedConfigFiles.toString();
        log.debug("Loaded config files: {}", internalConfig.loadedConfigFiles);

        internalConfig.parseUsernameFromGitEmailIfBlank();

        return internalConfig;
    }

    private static void applyRepoConfigFileIfExists(WorkflowConfig internalConfig, List<String> loadedConfigFiles) throws FileNotFoundException, IllegalAccessException {
        File repoDirectory = git.getRootDirectory();
        if (repoDirectory != null) {
            File repoWorkflowFile = new File(repoDirectory.getAbsolutePath() + File.separator + ".workflow-config.json");
            overrideConfigIfFileExists(internalConfig, repoWorkflowFile, loadedConfigFiles);
        }
    }

    private static void applyUserConfigFileIfExists(WorkflowConfig internalConfig, List<String> loadedConfigFiles) throws FileNotFoundException, IllegalAccessException {
        String homeFolder = System.getProperty("user.home");
        File userConfigFile = new File(homeFolder + File.separator + ".workflow-config.json");
        overrideConfigIfFileExists(internalConfig, userConfigFile, loadedConfigFiles);
    }

    private static void setLogLevel(WorkflowConfig internalConfig) {
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        LogLevel logLevelToUse = internalConfig.determineLogLevel();
        globalLogger.setLevel(logLevelToUse.getLevel());
        log.debug("Using log level {}", logLevelToUse);
    }

    private static void applyRuntimeArguments(WorkflowConfig config) throws IllegalAccessException {
        try {
            config.applyRuntimeArguments(argsParser);
        } catch (IllegalArgumentException iae) {
            // handle gracefully as they are validation type exceptions
            log.error(iae.getMessage());
            System.exit(1);
        }
    }

    /**
     * Applies values from configuration files explicitly specified either via the git workflow.config value or
     * via the command line.
     */
    private static void applySpecifiedConfigFiles(CommandLineArgumentsParser argsParser, WorkflowConfig internalConfig, List<String> loadedConfigFiles) throws IOException, IllegalAccessException {
        String gitConfigFilePath = git.configValue("workflow.config");
        String configFilePaths = argsParser.getArgument(gitConfigFilePath, "-c", "-config");
        if (StringUtils.isNotBlank(configFilePaths)) {
            String[] configFiles = configFilePaths.split(",");
            for (String configFilePath : configFiles) {
                File configFile = new File(configFilePath);
                WorkflowConfig overriddenConfig = readExternalWorkflowConfig(configFile);
                internalConfig.overrideValues(overriddenConfig, configFile.getName());
                loadedConfigFiles.add(configFile.getPath());
            }
        }
    }

    private static void overrideConfigIfFileExists(WorkflowConfig internalConfig, File repoWorkflowFile, List<String> loadedConfigFiles) throws FileNotFoundException, IllegalAccessException {
        if (!repoWorkflowFile.exists()) {
            return;
        }
        WorkflowConfig repoConfig = readExternalWorkflowConfig(repoWorkflowFile);
        internalConfig.overrideValues(repoConfig, repoWorkflowFile.getName());
        loadedConfigFiles.add(repoWorkflowFile.getPath());
    }

    private static WorkflowConfig readExternalWorkflowConfig(File configFilePath) throws FileNotFoundException {
        if (!configFilePath.exists()) {
            throw new IllegalArgumentException("Config file " + configFilePath.getPath() + " does not exist");
        }

        Reader externalConfigReader = new FileReader(configFilePath);
        try {
            return gson.fromJson(externalConfigReader, WorkflowConfig.class);
        } catch (JsonSyntaxException e) {
            log.error("Syntax error in external config file {}:\n{}", configFilePath.getPath(), e.getMessage());
            System.exit(1);
            return null;
        }
    }

    private static WorkflowConfig readInternalConfig() {
        Reader reader = new ClasspathResource("/internalConfig.json").getReader();
        return gson.fromJson(reader, WorkflowConfig.class);
    }
}
