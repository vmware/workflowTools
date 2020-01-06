package com.vmware.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vmware.config.commandLine.CommandLineArgumentsParser;
import com.vmware.config.section.PerforceClientConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.ClasspathResource;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.Perforce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses the workflow config from the source config files
 */
public class WorkflowConfigParser {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Git git = new Git();
    private final Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
    private final CommandLineArgumentsParser argsParser = new CommandLineArgumentsParser();

    public WorkflowConfig parseWorkflowConfig(String username, String[] args) {
        argsParser.generateArgumentMap(args);

        WorkflowConfig config = readInternalConfig();
        if (StringUtils.isNotEmpty(username)) {
            config.username = username;
        }

        // apply twice so that setting a debug log level can be detected earlier
        applyRuntimeArguments(config);
        config.setupLogLevel();

        String gitRemoteValue = git.configValue(String.format("remote.%s.url", config.gitRepoConfig.defaultGitRemote));
        config.setGitRemoteUrlAsReviewBoardRepo(gitRemoteValue);

        applyRepoConfigFileIfExists(config);
        applyUserConfigFileIfExists(config);
        applySpecifiedConfigFiles(argsParser, config);

        applyGitConfigValuesAsWorkflowConfigValues(config);

        applyRuntimeArguments(config);
        config.setupLogLevel();

        log.debug("Loaded config files: {}", config.getConfigurableFields().loadedConfigFilesText());

        parseUsernameIfBlank(config);

        log.trace("Workflow Config\n{}", gson.toJson(config));
        return config;
    }

    private void applyGitConfigValuesAsWorkflowConfigValues(WorkflowConfig config) {
        if (!git.workingDirectoryIsInGitRepo()) {
            return;
        }

        WorkflowFields configurableFields = config.getConfigurableFields();
        Map<String, String> gitConfigValues = git.configValues();
        configurableFields.applyGitConfigValues("", gitConfigValues);

        String trackingBranch = git.getTrackingBranch();
        String remoteName = trackingBranch != null ? trackingBranch.split("/")[0] : null;
        if (StringUtils.isNotEmpty(remoteName)) {
            configurableFields.setFieldValue("defaultGitRemote", remoteName, "tracking remote");

            log.debug("Applying remote specific config values for git remote {}", remoteName);
            configurableFields.applyGitConfigValues(remoteName, gitConfigValues);
            String trackingBranchConfigPrefix = trackingBranch.replace('/', '.');
            log.debug("Applying tracking branch specific config values for git tracking branch", trackingBranch);
            configurableFields.applyGitConfigValues(trackingBranchConfigPrefix, gitConfigValues);
        }
    }

    private void parseUsernameIfBlank(WorkflowConfig config) {
        if (StringUtils.isNotEmpty(config.username)) {
            return;
        }

        String[] parsedUsernameInfo = new UsernameParser().parse();
        if (parsedUsernameInfo != null) {
            config.setUsernameFromParsedValue(parsedUsernameInfo[0], parsedUsernameInfo[1]);
        } else {
            log.warn("Unable to determine username, some workflows might not work. Please use --username to set username");
        }
    }

    public void updateWithRuntimeArguments(WorkflowConfig config, String[] args) {
        argsParser.generateArgumentMap(args);
        applyRuntimeArguments(config);
        config.setupLogLevel();
    }

    public String getRuntimeArgumentsText() {
        return argsParser.getArgumentsText();
    }

    private void applyRuntimeArguments(WorkflowConfig config) {
        try {
            config.applyRuntimeArguments(argsParser);
        } catch (IllegalArgumentException iae) {
            // handle gracefully as they are validation type exceptions
            log.error(iae.getMessage());
            System.exit(1);
        }
    }

    /**
     * Applies values from configuration files explicitly specified either via the git workflow.configFile value or
     * via the command line.
     */
    private void applySpecifiedConfigFiles(CommandLineArgumentsParser argsParser, WorkflowConfig internalConfig) {
        String gitConfigFilePath = git.configValue("workflow.configFile");
        if (StringUtils.isEmpty(gitConfigFilePath)) {
            gitConfigFilePath = git.configValue("workflow.config"); // backwards compatibility
        }
        String configFilePaths = argsParser.getArgument(gitConfigFilePath, "-c", "-config", "--config");
        if (StringUtils.isEmpty(configFilePaths)) {
            return;
        }

        WorkflowFields fields = internalConfig.getConfigurableFields();
        String[] configFiles = configFilePaths.split(",");
        for (String configFilePath : configFiles) {
            File configFile = new File(configFilePath);
            WorkflowConfig overriddenConfig = readExternalWorkflowConfig(configFile);
            fields.overrideValues(overriddenConfig, configFile.getPath());
        }
    }

    private void applyRepoConfigFileIfExists(WorkflowConfig internalConfig) {
        File repoDirectory = git.getRootDirectory();
        if (repoDirectory == null) {
            PerforceClientConfig clientConfig = internalConfig.perforceClientConfig;
            Perforce perforce = new Perforce(clientConfig.perforceClientName, clientConfig.perforceClientDirectory);
            repoDirectory = perforce.getWorkingDirectory();
        }
        if (repoDirectory != null) {
            File repoWorkflowFile = new File(repoDirectory.getAbsolutePath() + File.separator + ".workflow-config.json");
            overrideConfigIfFileExists(internalConfig, repoWorkflowFile);
        }
    }

    private void applyUserConfigFileIfExists(WorkflowConfig internalConfig) {
        String homeFolder = System.getProperty("user.home");
        File userConfigFile = new File(homeFolder + File.separator + ".workflow-config.json");
        overrideConfigIfFileExists(internalConfig, userConfigFile);
    }

    private void overrideConfigIfFileExists(WorkflowConfig internalConfig, File repoWorkflowFile) {
        if (!repoWorkflowFile.exists()) {
            return;
        }
        WorkflowConfig repoConfig = readExternalWorkflowConfig(repoWorkflowFile);
        internalConfig.getConfigurableFields().overrideValues(repoConfig, repoWorkflowFile.getPath());
    }

    private WorkflowConfig readExternalWorkflowConfig(File configFilePath) {
        if (!configFilePath.exists()) {
            throw new FatalException("Config file {} does not exist", configFilePath.getPath());
        }

        try {
            return gson.fromJson(new FileReader(configFilePath), WorkflowConfig.class);
        } catch (JsonSyntaxException e) {
            throw new FatalException("Syntax error in external config file {}:\n{}",
                    configFilePath.getPath(), e.getMessage());
        } catch (FileNotFoundException e) {
            throw new RuntimeIOException(e);
        }
    }

    private WorkflowConfig readInternalConfig() {
        Reader reader = new ClasspathResource("/internalConfig.json", this.getClass()).getReader();
        return gson.fromJson(reader, WorkflowConfig.class);
    }
}
