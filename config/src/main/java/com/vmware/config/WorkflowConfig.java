package com.vmware.config;

import com.google.gson.annotations.Expose;
import com.vmware.config.commandLine.CommandLineArgumentsParser;
import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.config.section.BugzillaConfig;
import com.vmware.config.section.BuildwebConfig;
import com.vmware.config.section.CheckstyleConfig;
import com.vmware.config.section.CommandLineConfig;
import com.vmware.config.section.CommitConfig;
import com.vmware.config.section.CommitStatsConfig;
import com.vmware.config.section.FileSystemConfig;
import com.vmware.config.section.GitRepoConfig;
import com.vmware.config.section.GitlabConfig;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.config.section.JiraConfig;
import com.vmware.config.section.LoggingConfig;
import com.vmware.config.section.PatchConfig;
import com.vmware.config.section.PerforceClientConfig;
import com.vmware.config.section.ReviewBoardConfig;
import com.vmware.config.section.SectionConfig;
import com.vmware.config.section.SshConfig;
import com.vmware.config.section.TrelloConfig;
import com.vmware.config.section.VcdConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;
import com.vmware.util.logging.LogLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Workflow configuration.
 * All configuration is contained in this class.
 */
public class WorkflowConfig {

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String COMMAND_LINE_SOURCE = "Command Line";

    @SectionConfig
    public LoggingConfig loggingConfig;

    @SectionConfig
    public GitRepoConfig gitRepoConfig;

    @SectionConfig
    public GitlabConfig gitlabConfig;

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

    @SectionConfig
    public SshConfig sshConfig;

    @SectionConfig
    public VcdConfig vcdConfig;

    @SectionConfig
    public CommandLineConfig commandLineConfig;

    @SectionConfig
    public FileSystemConfig fileSystemConfig;

    @ConfigurableProperty(help = "Information about the the git commit that this version of workflow tools was built from")
    public Map<String, String> buildInfo;

    @ConfigurableProperty(commandLine = "-u,--username", help = "Username to use for jenkins, jira, review board and vcd")
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

    @ConfigurableProperty(commandLine = "-cp,--check-point", help = "Check after every workflow action whether to continue")
    public boolean checkPoint;

    @ConfigurableProperty(commandLine = "-sp,--specific-properties", help = "Show value for just the specified config properties")
    public String configPropertiesToDisplay;

    @ConfigurableProperty(commandLine = "-wait,--wait-for-action", help = "Wait for blocking workflow action to complete")
    public boolean waitForBlockingWorkflowAction;

    @ConfigurableProperty(commandLine = "--wait-time", help = "Wait time in seconds for blocking workflow action to complete.")
    public int waitTimeForBlockingWorkflowAction;

    @ConfigurableProperty(commandLine = "--ignore-unknown-actions", help = "Ignore unknown workflow actions")
    public boolean ignoreUnknownActions;

    @ConfigurableProperty(commandLine = "-s,--shell", help = "Run workflow in shell mode, will allow additional commands to be run")
    public boolean shellMode;

    @ConfigurableProperty(commandLine = "--output-file", help = "File to store output to")
    public String outputFile;

    @ConfigurableProperty(help = "Project documentation url")
    public String projectDocumentationUrl;

    @Expose(serialize = false, deserialize = false)
    public ReplacementVariables replacementVariables = new ReplacementVariables(this);

    @Expose(serialize = false, deserialize = false)
    private WorkflowFields configurableFields;

    public WorkflowConfig() {
        this.configurableFields = new WorkflowFields(this);
    }

    public void setupLogLevel() {
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        LogLevel logLevelToUse = loggingConfig.determineLogLevel();
        globalLogger.setLevel(logLevelToUse.getLevel());
        log.debug("Using log level {}", logLevelToUse);
    }

    public void applyRuntimeArguments(CommandLineArgumentsParser argsParser) {
        List<ConfigurableProperty> commandLineProperties = applyConfigValues(argsParser.getArgumentMap(), COMMAND_LINE_SOURCE, true);
        if (argsParser.containsArgument("--possible-workflow")) {
            configurableFields.markFieldAsOverridden("workflowsToRun", "Command Line");
            this.workflowsToRun = argsParser.getExpectedArgument("--possible-workflow");
        }
        argsParser.checkForUnrecognizedArguments(commandLineProperties);
    }

    public Map<String, CalculatedProperty> getExistingValues(Set<String> configValues) {
        Map<String, CalculatedProperty> existingValues = new HashMap<>();
        for (String configValueName : configValues) {
            if (configValueName.startsWith(JenkinsConfig.CONFIG_PREFIX)) {
                if (!existingValues.containsKey("jenkinsJobParameters")) {
                    existingValues.put("jenkinsJobParameters", new CalculatedProperty(new HashMap<>(jenkinsConfig.jenkinsJobParameters),
                            configurableFields.getFieldValueSource("jenkinsJobParameters")));
                }
                continue;
            }
            List<WorkflowField> matchingFields = configurableFields.findWorkflowFieldsByConfigValue(configValueName);
            for (WorkflowField matchingField : matchingFields) {
                if (!existingValues.containsKey(matchingField.getName())) {
                    existingValues.put(matchingField.getName(), valueForField(matchingField));
                }
            }
        }
        return existingValues;
    }

    public List<ConfigurableProperty> applyConfigValues(Map<String, String> configValues, String source, boolean overwriteJenkinsParameters) {
        if (configValues.isEmpty()) {
            return Collections.emptyList();
        }
        jenkinsConfig.addJenkinsParametersFromConfigValues(configValues, overwriteJenkinsParameters);
        replacementVariables.addReplacementVariables(configValues, COMMAND_LINE_SOURCE.equals(source));
        return configurableFields.applyConfigValues(configValues, source);
    }

    public void applyValuesWithSource(Map<String, CalculatedProperty> values) {
        values.forEach((key, value) -> configurableFields.setFieldValue(key, value.getValue(), value.getSource()));
    }

    /**
     * Set separate to other git config values as it shouldn't override a specific workflow file configuration.
     */
    public void setGitRemoteUrlAsReviewBoardRepo(String gitRemoteValue) {
        if (StringUtils.isEmpty(gitRemoteValue)) {
            return;
        }

        log.debug("Setting git remote value {} as the reviewboard repository, will be overridden if a config value is specified", gitRemoteValue);
        reviewBoardConfig.reviewBoardRepository = gitRemoteValue;
    }

    public void setUsernameFromParsedValue(String username, String source) {
        this.username = username;
        configurableFields.markFieldAsOverridden("username", source);
    }

    public JenkinsJobsConfig getJenkinsJobsConfig() {
        return jenkinsConfig.getJenkinsJobsConfig(this.username, gitRepoConfig.determineBranchName());
    }

    public WorkflowFields getConfigurableFields() {
        return configurableFields;
    }

    public CalculatedProperty valueForField(WorkflowField matchingField) {
        ConfigurableProperty configurableProperty = matchingField.configAnnotation();
        if (StringUtils.isEmpty(configurableProperty.methodNameForValueCalculation())) {
            Object value = matchingField.getValue(this);
            return new CalculatedProperty(value, configurableFields.getFieldValueSource(matchingField.getName()));
        }
        Class sectionConfigClass = matchingField.getConfigClassContainingField();
        try {
            Method methodToExecute = sectionConfigClass.getMethod(configurableProperty.methodNameForValueCalculation());
            return (CalculatedProperty) methodToExecute.invoke(sectionConfigForClass(sectionConfigClass));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }

    public void applyReplacementVariables() {
        if (replacementVariables.isEmpty()) {
            return;
        }
        WorkflowFields fields = getConfigurableFields();
        List<WorkflowField> fieldsWithStringType = fields.values().stream().filter(field -> field.getType() == String.class).collect(Collectors.toList());
        for (WorkflowField field : fieldsWithStringType) {
            String value = (String) valueForField(field).getValue();
            String updatedValue = replaceVariablesInValue(value);

            if (value != null && !value.equals(updatedValue)) {
                Class sectionConfigClass = field.getConfigClassContainingField();
                field.setValue(sectionConfigForClass(sectionConfigClass), updatedValue);
            }
        }
    }

    private String replaceVariablesInValue(String value) {
        if (value == null || !value.contains("$")) {
            return value;
        }

        String existingValue;
        String updatedValue = value;
        do {
            existingValue = updatedValue;
            updatedValue = replacementVariables.replaceVariablesInValue(existingValue);
            if (!existingValue.equals(updatedValue)) {
                log.trace(existingValue + " " + updatedValue);
            }
        } while(!existingValue.equals(updatedValue));
        return updatedValue;
    }

    public List<WorkflowParameter> applyReplacementVariables(List<WorkflowParameter> parameters) {
        List<WorkflowParameter> updatedParams = new ArrayList<>();
        for (WorkflowParameter parameter : parameters) {
            String updatedValue = replaceVariablesInValue(parameter.getValue());
            if (parameter.getValue() != null && !parameter.getValue().equals(updatedValue)) {
                updatedParams.add(new WorkflowParameter(parameter.getName(), updatedValue));
            } else {
                updatedParams.add(parameter);
            }
        }
        return updatedParams;
    }

    private Object sectionConfigForClass(Class clazz) {
        if (clazz == WorkflowConfig.class) {
            return this;
        }
        Optional<Field> matchingField = Arrays.stream(this.getClass().getFields()).filter(field -> field.getType().equals(clazz)).findFirst();
        if (!matchingField.isPresent()) {
            throw new FatalException("No match for config class {}", clazz.getName());
        }
        try {
            return matchingField.get().get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeReflectiveOperationException(e);
        }
    }
}