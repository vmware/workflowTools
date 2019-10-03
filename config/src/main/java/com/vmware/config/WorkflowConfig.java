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
import com.vmware.config.section.SshConfig;
import com.vmware.config.section.TrelloConfig;
import com.vmware.config.section.VcdConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeReflectiveOperationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Workflow configuration.
 * All configuration is contained in this class.
 */
public class WorkflowConfig {

    private Logger log = LoggerFactory.getLogger(this.getClass());

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

    @SectionConfig
    public SshConfig sshConfig;

    @SectionConfig
    public VcdConfig vcdConfig;

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

    @ConfigurableProperty(commandLine = "-cp,--specific-properties", help = "Show value for just the specified config properties")
    public String configPropertiesToDisplay;

    @ConfigurableProperty(commandLine = "--wait-time", help = "Wait time in seconds for blocking workflow action to complete.")
    public int waitTimeForBlockingWorkflowAction;

    @ConfigurableProperty(commandLine = "--ignore-unknown-actions", help = "Ignore unknown workflow actions")
    public boolean ignoreUnknownActions;

    @Expose(serialize = false, deserialize = false)
    private WorkflowFields configurableFields;

    public WorkflowConfig() {
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

    public List<ConfigurableProperty> applyConfigValues(Map<String, String> configValues, String source, boolean overwriteJenkinsParameters) {
        if (configValues.isEmpty()) {
            return Collections.emptyList();
        }
        jenkinsConfig.addJenkinsParametersFromConfigValues(configValues, overwriteJenkinsParameters);
        return configurableFields.applyConfigValues(configValues, source);
    }

    /**
     * Set separate to other git config values as it shouldn't override a specific workflow file configuration.
     */
    public void setGitRemoteUrlAsReviewBoardRepo(String gitRemoteValue) {
        if (StringUtils.isBlank(gitRemoteValue)) {
            return;
        }

        log.debug("Setting git remote value {} as the reviewboard repository", gitRemoteValue);
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

    public Object sectionConfigForClass(Class clazz) {
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