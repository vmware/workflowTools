package com.vmware.config;

import com.google.gson.annotations.Expose;
import com.vmware.config.section.BuildwebConfig;
import com.vmware.config.section.PatchConfig;
import com.vmware.config.section.PerforceClientConfig;
import com.vmware.config.section.SectionConfig;
import com.vmware.config.section.BugzillaConfig;
import com.vmware.config.section.CheckstyleConfig;
import com.vmware.config.section.CommitConfig;
import com.vmware.config.section.CommitStatsConfig;
import com.vmware.config.section.GitRepoConfig;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.config.section.JenkinsJobsConfig;
import com.vmware.config.section.JiraConfig;
import com.vmware.config.section.ReviewBoardConfig;
import com.vmware.config.section.TrelloConfig;
import com.vmware.config.commandLine.CommandLineArgumentsParser;
import com.vmware.util.ArrayUtils;
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

/**
 * Workflow configuration.
 * All configuration is contained in this class.
 */
public class WorkflowConfig {

    private static Logger log = LoggerFactory.getLogger(WorkflowConfig.class.getName());

    @Expose(serialize = false, deserialize = false)
    private Git git = new Git();

    @Expose(serialize = false, deserialize = false)
    private Map<String, String> overriddenConfigSources = new TreeMap<>();

    @Expose(serialize = false, deserialize = false)
    public List<Field> configurableFields = new ArrayList<>();

    @Expose(serialize = false, deserialize = false)
    public String loadedConfigFiles;

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
    public JenkinsJobsConfig jenkinsJobsConfig;

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

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(help = "Order of services to check against for bug number")
    public String[] bugNumberSearchOrder;

    @ConfigurableProperty(commandLine = "-t,--trace", help = "Sets log level to trace")
    public boolean traceLogLevel;

    @ConfigurableProperty(commandLine = "-d,--debug", help = "Sets log level to debug")
    public boolean debugLogLevel;

    @ConfigurableProperty(commandLine = "-l, ,--log, --log-level", help = "Sets log level to any of the following, SEVERE,INFO,FINE,FINER,FINEST")
    public String logLevel;

    @ConfigurableProperty(help = "A map of workflows that can be configured. A workflow comprises a list of workflow actions.")
    public TreeMap<String, List<String>> workflows;

    @ConfigurableProperty(help = "A list of workflows that are only created for supporting other workflows. Adding them here hides them on initial auto complete")
    public List<String> supportingWorkflows;

    @ConfigurableProperty(commandLine = "-w,--workflow", help = "Workflows / Actions to run")
    public String workflowsToRun;

    @ConfigurableProperty(commandLine = "-dr,--dry-run", help = "Shows the workflow actions that would be run")
    public boolean dryRun;

    @ConfigurableProperty(commandLine = "--set-empty-only", help = "Set values for empty properties only. Ignore properties that already have values")
    public boolean setEmptyPropertiesOnly;

    @ConfigurableProperty(commandLine = "-cp,--specific-properties", help = "Show value for just the specified config properties")
    public String configPropertiesToDisplay;

    @ConfigurableProperty(commandLine = "--wait-time", help = "Wait time in seconds for blocking workflow action to complete.")
    public int waitTimeForBlockingWorkflowAction;

    public WorkflowConfig() {}

    public void generateConfigurablePropertyList() {
        Map<String, Field> usedParams = new HashMap<>();
        addConfigurablePropertiesForClass(WorkflowConfig.class, usedParams);

        Arrays.stream(WorkflowConfig.class.getFields()).filter(field -> field.getAnnotation(SectionConfig.class) != null)
                .forEach(field -> addConfigurablePropertiesForClass(field.getType(), usedParams));
    }

    public void applyRuntimeArguments(CommandLineArgumentsParser argsParser) {
        List<ConfigurableProperty> commandLineProperties = applyConfigValues(argsParser.getArgumentMap(), "Command Line", true);
        if (argsParser.containsArgument("--possible-workflow")) {
            overriddenConfigSources.put("workflowsToRun", "Command Line");
            this.workflowsToRun = argsParser.getExpectedArgument("--possible-workflow");
        }
        argsParser.checkForUnrecognizedArguments(commandLineProperties);
    }

    public LogLevel determineLogLevel() {
        if (traceLogLevel) {
            logLevel = LogLevel.TRACE.name();
        } else if (debugLogLevel) {
            logLevel = LogLevel.DEBUG.name();
        }
        return LogLevel.valueOf(logLevel);
    }

    public int getSearchOrderForService(String serviceToCheckFor) {
        for (int i = 0; i < bugNumberSearchOrder.length; i ++) {
            if (bugNumberSearchOrder[i].equalsIgnoreCase(serviceToCheckFor)) {
               return i;
            }
        }
        return -1;
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
        setFieldValue(ReflectionUtils.getField(GitRepoConfig.class, "defaultGitRemote"), remoteName, "tracking remote");
    }

    public void applyGitConfigValues(String configPrefix) {
        String configPrefixText = StringUtils.isBlank(configPrefix) ? "" : configPrefix + ".";
        Map<String, String> configValues = git.configValues();
        String sourceConfigProperty;
        for (Field field : configurableFields) {
            ConfigurableProperty configurableProperty = field.getAnnotation(ConfigurableProperty.class);
            String workflowConfigPropertyName = "workflow." + configPrefixText + field.getName().toLowerCase();
            String gitConfigPropertyName = configurableProperty.gitConfigProperty();
            String valueToSet = null;
            if (!gitConfigPropertyName.isEmpty() && StringUtils.isBlank(configPrefix)) {
                String valueByGitConfig = configValues.get(gitConfigPropertyName);
                String valueByWorkflowProperty = configValues.get(workflowConfigPropertyName);
                if (valueByGitConfig != null && valueByWorkflowProperty != null && !valueByGitConfig.equals(valueByGitConfig)) {
                    throw new FatalException("Property {} has value {} specified by the git config property {}" +
                            " but has value {} specified by the workflow property {}, please remove one of the properties",
                            field.getName(), valueByGitConfig, gitConfigPropertyName, valueByWorkflowProperty,
                            workflowConfigPropertyName);
                }
                sourceConfigProperty = valueByGitConfig != null ? gitConfigPropertyName : workflowConfigPropertyName;
                valueToSet = valueByGitConfig != null ? valueByGitConfig : valueByWorkflowProperty;
            } else {
                sourceConfigProperty = workflowConfigPropertyName;
                valueToSet = configValues.get(workflowConfigPropertyName);
            }

            setFieldValue(field, valueToSet, "Git " + sourceConfigProperty);
        }
    }

    public void overrideValues(WorkflowConfig overriddenConfig, String configFileName) {
        for (Field field : configurableFields) {
            Object existingValue = ReflectionUtils.getValue(field, this);
            Object value = ReflectionUtils.getValue(field, overriddenConfig);
            if (value == null || String.valueOf(value).equals("0") || (value instanceof Boolean && !((Boolean) value))) {
                continue;
            }
            // copy values to default config map if value is a map
            if (existingValue != null && value instanceof Map) {
                Map valueMap = (Map) value;
                if (valueMap.isEmpty()) {
                    continue;
                }
                Map existingValues = (Map) existingValue;
                String existingConfigValue = overriddenConfigSources.get(field.getName());
                String updatedConfigValue;
                if (existingConfigValue == null && !existingValues.isEmpty()) {
                    updatedConfigValue = "default, " + configFileName;
                } else if (existingConfigValue == null) {
                    updatedConfigValue = configFileName;
                } else {
                    updatedConfigValue = existingConfigValue + ", " + configFileName;
                }
                overriddenConfigSources.put(field.getName(), updatedConfigValue);
                existingValues.putAll(valueMap);
            } else {
                overriddenConfigSources.put(field.getName(), configFileName);
                // override for everything else
                ReflectionUtils.setValue(field, this, value);
            }
        }
    }

    public void parseUsernameFromGitEmailIfBlank() {
        if (StringUtils.isNotBlank(username)) {
            return;
        }
        String gitUserEmail = git.configValue("user.email");
        if (StringUtils.isNotBlank(gitUserEmail) && gitUserEmail.contains("@")) {
            this.username = gitUserEmail.substring(0, gitUserEmail.indexOf("@"));
            log.info("No username set, parsed username {} from git config user.email {}", username, gitUserEmail);
            overriddenConfigSources.put("username", "Git user.email");
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
            overriddenConfigSources.put("username", "Perforce user");
        }
    }

    public void parseUsernameFromWhoamIIfBlank() {
        if (StringUtils.isNotBlank(username)) {
            return;
        }
        String fullUsername = CommandLineUtils.executeCommand("whoami", LogLevel.DEBUG);
        String[] usernamePieces = fullUsername.split("\\\\");
        this.username = usernamePieces[usernamePieces.length - 1];
        log.info("No username set, parsed username {} from whoami output {}", username, fullUsername);
        overriddenConfigSources.put("username", "Whoami command");
    }

    public Field getMatchingField(String commandLineProperty) {
        for (Field field : configurableFields) {
            ConfigurableProperty property = field.getAnnotation(ConfigurableProperty.class);
            if (property.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }

            if (ArrayUtils.contains(property.commandLine().split(","), commandLineProperty)) {
                return field;
            }
        }
        return null;
    }

    public String getFieldValueSource(String fieldName) {
        String value = overriddenConfigSources.get(fieldName);
        return value != null ? value : "default";
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
            if (!overwriteJenkinsParameters && jenkinsJobsConfig.jenkinsJobParameters.containsKey(parameterName)) {
                log.debug("Ignoring config value {} as it is already set", configValue);
                continue;
            }
            jenkinsJobsConfig.jenkinsJobParameters.put(parameterName, parameterValue);
        }
        List<ConfigurableProperty> propertiesAffected = new ArrayList<>();
        for (Field field : configurableFields) {
            ConfigurableProperty configurableProperty = field.getAnnotation(ConfigurableProperty.class);
            if (configurableProperty.commandLine().equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                continue;
            }
            for (String configValue : configValues.keySet()) {
                String[] commandLineArguments = configurableProperty.commandLine().split(",");
                if (ArrayUtils.contains(commandLineArguments, configValue)) {
                    propertiesAffected.add(configurableProperty);
                    String value = configValues.get(configValue);
                    if (value == null && (field.getType() == Boolean.class || field.getType() == boolean.class)) {
                        value = Boolean.TRUE.toString();
                    }
                    setFieldValue(field, value, source);
                }
            }
        }
        return propertiesAffected;
    }

    private void addConfigurablePropertiesForClass(Class classToCheck, Map<String, Field> usedParams) {
        for (Field field : classToCheck.getFields()) {
            ConfigurableProperty configProperty = field.getAnnotation(ConfigurableProperty.class);
            if (configProperty == null) {
                continue;
            }
            String[] params = configProperty.commandLine().split(",");
            for (String param : params) {
                if (param.equals(ConfigurableProperty.NO_COMMAND_LINE_OVERRIDES)) {
                    continue;
                }
                boolean fieldNameAlreadyUsed = configurableFields.stream()
                        .anyMatch(existingField -> existingField.getName().equals(field.getName()));
                if (!fieldNameAlreadyUsed && usedParams.containsKey(param)) {
                    throw new FatalException(
                            "Config flag {} has already been set to configure another property {}", param,
                            usedParams.get(param).getName());
                }
                usedParams.put(param, field);
            }
            configurableFields.add(field);
        }
    }

    private void setFieldValue(Field field, String value, String source) {
        Object validValue = new WorkflowField(field).determineValue(value);
        if (validValue != null) {
            overriddenConfigSources.put(field.getName(), source);
            ReflectionUtils.setValue(field, this, validValue);
        }
    }
}
