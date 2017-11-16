package com.vmware.config;

import com.google.gson.annotations.Expose;
import com.vmware.config.commandLine.CommandLineArgumentsParser;
import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.config.section.BugzillaConfig;
import com.vmware.config.section.BuildwebConfig;
import com.vmware.config.section.CheckstyleConfig;
import com.vmware.config.section.CommitConfig;
import com.vmware.config.section.CommitStatsConfig;
import com.vmware.config.section.GitRepoConfig;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.config.section.JiraConfig;
import com.vmware.config.section.LoggingConfig;
import com.vmware.config.section.PatchConfig;
import com.vmware.config.section.PerforceClientConfig;
import com.vmware.config.section.ReviewBoardConfig;
import com.vmware.config.section.SectionConfig;
import com.vmware.config.section.TrelloConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.ReflectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.Perforce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Workflow configuration.
 * All configuration is contained in this class.
 */
public class WorkflowConfig {

    private static Logger log = LoggerFactory.getLogger(WorkflowConfig.class.getName());

    @Expose(serialize = false, deserialize = false)
    private Git git = new Git();

    @Expose(serialize = false, deserialize = false)
    public String loadedConfigFiles;

    @SectionConfig
    public LoggingConfig loggingConfig;

    @SectionConfig
    public GitRepoConfig gitRepoConfig;

    @SectionConfig
    public PerforceClientConfig perforceClientConfig;

    @SectionConfig
    public CommitConfig commitConfig;

    @SectionConfig
    public ReviewBoardConfig reviewBoardConfig;

    @SectionConfig
    public CommitStatsConfig statsConfig;

    @SectionConfig
    public JiraConfig jiraConfig;

    @SectionConfig
    public BugzillaConfig bugzillaConfig;

    @SectionConfig
    public JenkinsConfig jenkinsConfig;

    @SectionConfig
    public TrelloConfig trelloConfig;

    @SectionConfig
    public CheckstyleConfig checkstyleConfig;

    @SectionConfig
    public PatchConfig patchConfig;

    @SectionConfig
    public BuildwebConfig buildwebConfig;

    @ConfigurableProperty(help = "Information about the the git commit that this version of workflow tools was built from")
    public Map<String, String> buildInfo;

    @ConfigurableProperty(commandLine = "-u,--username", help = "Username to use for jenkins, jira and review board")
    public String username;

    @ConfigurableProperty(help = "Order of services to check against for bug number")
    public List<String> bugNumberSearchOrder;

    @ConfigurableProperty(help = "A map of workflows that can be configured. A workflow comprises a list of workflow actions.")
    public TreeMap<String, List<String>> workflows;

    @ConfigurableProperty(help = "A list of workflows that are only created for supporting other workflows. Adding them here hides them on initial auto complete")
    public List<String> supportingWorkflows;

    @ConfigurableProperty(commandLine = "-w,--workflow", help = "Workflows / Actions to run")
    public String workflowsToRun;

    @ConfigurableProperty(commandLine = "-dr,--dry-run", help = "Shows the workflow actions that would be run")
    public boolean dryRun;

    @ConfigurableProperty(commandLine = "-cp,--specific-properties", help = "Show value for just the specified config properties")
    public String configPropertiesToDisplay;

    @ConfigurableProperty(commandLine = "--wait-time", help = "Wait time in seconds for blocking workflow action to complete.")
    public int waitTimeForBlockingWorkflowAction;

    @Expose(serialize = false, deserialize = false)
    private WorkflowFields configurableFields;

    public WorkflowConfig() {}

    public void generateConfigurableFieldList() {
        this.configurableFields = new WorkflowFields(this);
    }

    public void applyRuntimeArguments(CommandLineArgumentsParser argsParser) {
        List<ConfigurableProperty> commandLineProperties = applyConfigValues(argsParser.getArgumentMap(), "Command Line", true);
        if (argsParser.containsArgument("--possible-workflow")) {
            configurableFields.markFieldAsOverridden("workflowsToRun", "Command Line");
            this.workflowsToRun = argsParser.getExpectedArgument("--possible-workflow");
        }
        argsParser.checkForUnrecognizedArguments(commandLineProperties);
    }

    /**
     * Set separate to other git config values as it shouldn't override a specific workflow file configuration.
     */
    public void setGitRemoteUrlAsReviewBoardRepo() {
        String gitRemoteValue = git.configValue("remote." + gitRepoConfig.defaultGitRemote + ".url");
        if (StringUtils.isBlank(gitRemoteValue)) {
            return;
        }

        log.debug("Setting git remote value {} as the reviewboard repository", gitRemoteValue);
        reviewBoardConfig.reviewBoardRepository = gitRemoteValue;
    }

    public void setDefaultGitRemoteFromTrackingRemote(String remoteName) {
        configurableFields.setFieldValue("defaultGitRemote", remoteName, "tracking remote");
    }

    public void parseUsernameFromGitEmailIfBlank() {
        if (StringUtils.isNotBlank(username)) {
            return;
        }
        String gitUserEmail = git.configValue("user.email");
        if (StringUtils.isNotBlank(gitUserEmail) && gitUserEmail.contains("@")) {
            this.username = gitUserEmail.substring(0, gitUserEmail.indexOf("@"));
            log.info("No username set, parsed username {} from git config user.email {}", username, gitUserEmail);
            configurableFields.markFieldAsOverridden("username", "Git user.email");
        }
    }

    public void parseUsernameFromPerforceIfBlank() {
        if (StringUtils.isNotBlank(username)) {
            return;
        }
        Perforce perforce = new Perforce(System.getProperty("user.dir"));
        if (perforce.isLoggedIn()) {
            this.username = perforce.getUsername();
            log.info("No username set, using perforce user {} as username", username);
            configurableFields.markFieldAsOverridden("username", "Perforce user");
        }
    }

    public void parseUsernameFromWhoamIIfBlank() {
        if (StringUtils.isNotBlank(username) || !CommandLineUtils.isCommandAvailable("whoami")) {
            return;
        }
        String fullUsername = CommandLineUtils.executeCommand("whoami", LogLevel.DEBUG);
        String[] usernamePieces = fullUsername.split("\\\\");
        this.username = usernamePieces[usernamePieces.length - 1];
        log.info("No username set, parsed username {} from whoami output {}", username, fullUsername);
        configurableFields.markFieldAsOverridden("username", "Whoami command");
    }

    public List<ConfigurableProperty> applyConfigValues(Map<String, String> configValues, String source, boolean overwriteJenkinsParameters) {
        if (configValues.isEmpty()) {
            return Collections.emptyList();
        }
        for (String configValue : configValues.keySet()) {
            if (!configValue.startsWith("--J")) {
                continue;
            }
            String parameterName = configValue.substring(3);
            String parameterValue = configValues.get(configValue);
            if (!overwriteJenkinsParameters && jenkinsConfig.jenkinsJobParameters.containsKey(parameterName)) {
                log.debug("Ignoring config value {} as it is already set", configValue);
                continue;
            }
            jenkinsConfig.jenkinsJobParameters.put(parameterName, parameterValue);
        }
        return configurableFields.applyConfigValues(configValues, source);
    }

    public JenkinsJobsConfig getJenkinsJobsConfig() {
        return jenkinsConfig.getJenkinsJobsConfig(this.username);
    }

    public void applyGitConfigValues(String configPrefix) {
        configurableFields.applyGitConfigValues(configPrefix, git.configValues());
    }

    public WorkflowFields getConfigurableFields() {
        return configurableFields;
    }
}
