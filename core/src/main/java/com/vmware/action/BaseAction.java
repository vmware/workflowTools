package com.vmware.action;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.vmware.ServiceLocator;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.WorkflowField;
import com.vmware.config.WorkflowFields;
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
import com.vmware.config.section.PatchConfig;
import com.vmware.config.section.PerforceClientConfig;
import com.vmware.config.section.ReviewBoardConfig;
import com.vmware.config.section.SshConfig;
import com.vmware.config.section.TrelloConfig;
import com.vmware.config.section.VcdConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.CancelException;
import com.vmware.util.exception.SkipActionException;
import com.vmware.util.exception.FatalException;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.scm.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAction implements Action {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final GitRepoConfig gitRepoConfig;
    protected final GitlabConfig gitlabConfig;
    protected final PerforceClientConfig perforceClientConfig;
    protected final CommitConfig commitConfig;
    protected final CommitStatsConfig statsConfig;
    protected final WorkflowConfig config;
    protected final ReviewBoardConfig reviewBoardConfig;
    protected final JiraConfig jiraConfig;
    protected final BugzillaConfig bugzillaConfig;
    protected final JenkinsConfig jenkinsConfig;
    protected final TrelloConfig trelloConfig;
    protected final CheckstyleConfig checkstyleConfig;
    protected final PatchConfig patchConfig;
    protected final BuildwebConfig buildwebConfig;
    protected final SshConfig sshConfig;
    protected final VcdConfig vcdConfig;
    protected final CommandLineConfig commandLineConfig;
    protected final FileSystemConfig fileSystemConfig;

    protected ServiceLocator serviceLocator;

    protected Git git;

    protected boolean failIfCannotBeRun;

    protected ReplacementVariables replacementVariables;

    private Set<String> failWorkflowIfBlankProperties = new HashSet<>();

    private String[] expectedCommandsToBeAvailable;

    private Set<String> skipActionIfBlankProperties = new HashSet<>();


    public BaseAction(WorkflowConfig config) {
        this.config = config;
        this.commitConfig = config.commitConfig;
        this.statsConfig = config.statsConfig;
        this.gitRepoConfig = config.gitRepoConfig;
        this.gitlabConfig = config.gitlabConfig;
        this.perforceClientConfig = config.perforceClientConfig;
        this.reviewBoardConfig = config.reviewBoardConfig;
        this.jiraConfig = config.jiraConfig;
        this.bugzillaConfig = config.bugzillaConfig;
        this.jenkinsConfig = config.jenkinsConfig;
        this.trelloConfig = config.trelloConfig;
        this.checkstyleConfig = config.checkstyleConfig;
        this.patchConfig = config.patchConfig;
        this.buildwebConfig = config.buildwebConfig;
        this.sshConfig = config.sshConfig;
        this.vcdConfig = config.vcdConfig;
        this.commandLineConfig = config.commandLineConfig;
        this.fileSystemConfig = config.fileSystemConfig;
        this.replacementVariables = config.replacementVariables;
    }

    public void checkIfWorkflowShouldBeFailed() {
        checkExpectedCommands();
        failWorkflowIfBlankProperties.forEach(this::failIfUnset);
        if (failIfCannotBeRun) {
            try {
                this.checkIfActionShouldBeSkipped();
            } catch (SkipActionException ce) {
                throw new FatalException(ce, ce.getMessage());
            }

        }
        failWorkflowIfConditionNotMet();
    }


    protected void failWorkflowIfConditionNotMet() {
    }

    @Override
    public void asyncSetup() {
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        skipActionIfBlankProperties.forEach(this::skipActionIfUnset);
    }

    @Override
    public void preprocess() {
    }

    public void checkSkipAndFailConfigPropertiesAreValid() {
        WorkflowFields fields = config.getConfigurableFields();
        skipActionIfBlankProperties.forEach(propertyName -> checkPropertyMatchesConfigValue(fields, propertyName));
        failWorkflowIfBlankProperties.forEach(propertyName -> checkPropertyMatchesConfigValue(fields, propertyName));
    }

    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        this.git = serviceLocator.getGit();
    }

    protected void setExpectedCommandsToBeAvailable(String... commands) {
        this.expectedCommandsToBeAvailable = commands;
    }

    protected void addFailWorkflowIfBlankProperties(String... propertyNames) {
        failWorkflowIfBlankProperties.addAll(Arrays.asList(propertyNames));
    }

    protected void addSkipActionIfBlankProperties(String... propertyNames) {
        skipActionIfBlankProperties.addAll(Arrays.asList(propertyNames));
    }

    protected void cancelWithMessage(String message, String... arguments) {
        throw new CancelException(LogLevel.INFO, message, arguments);
    }

    protected void cancelWithErrorMessage(String message) {
        throw new FatalException(message);
    }

    protected void cancelWithWarnMessage(String message) {
        throw new CancelException(LogLevel.WARN, message);
    }

    private void checkExpectedCommands() {
        if (expectedCommandsToBeAvailable == null) {
            return;
        }
        Arrays.stream(expectedCommandsToBeAvailable)
                .forEach(command -> failIfTrue(!CommandLineUtils.isCommandAvailable(command), "command " + command + " is not available"));
    }

    protected void exitDueToFailureCheck(String reason) {
        cancelWithErrorMessage("Workflow failed by action " + this.getClass().getSimpleName() + " as " + reason);
    }

    protected void skipActionDueTo(String reason, Object... arguments) {
        throw new SkipActionException(reason, arguments);
    }

    protected void skipActionIfTrue(boolean propertyValue, String description) {
        if (propertyValue) {
            skipActionDueTo(description);
        }
    }

    protected void skipActionIfUnset(String propertyName) {
        WorkflowField matchingField = config.getConfigurableFields().getFieldByName(propertyName);
        if (matchingField.getValue(config) == null) {
            String commandLineParam = matchingField.commandLineParameter();
            String paramText = commandLineParam != null ? " (" + commandLineParam + ") " : "";
            throw new SkipActionException("{}{} is unset", propertyName, paramText);
        }
    }

    protected void failIfTrue(boolean value, String description) {
        if (value) {
            exitDueToFailureCheck(description);
        }
    }

    protected void failIfUnset(String propertyName) {
        WorkflowField matchingField = config.getConfigurableFields().getFieldByName(propertyName);
        if (matchingField.getValue(config) == null) {
            String commandLineParam = matchingField.commandLineParameter();
            String paramText = commandLineParam != null ? " (" + commandLineParam + ") " : "";
            exitDueToFailureCheck("property " + propertyName + paramText + " not set");
        }
    }

    private void checkPropertyMatchesConfigValue(WorkflowFields fields, String propertyName) {
        try {
            fields.getFieldByName(propertyName);
        } catch (RuntimeException re) {
            throw new RuntimeException("Failed to find config value " + propertyName + " in class " + this.getClass().getSimpleName(), re);
        }
    }
}
