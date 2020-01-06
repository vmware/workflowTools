package com.vmware.action;

import com.vmware.ServiceLocator;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.section.BugzillaConfig;
import com.vmware.config.section.BuildwebConfig;
import com.vmware.config.section.CheckstyleConfig;
import com.vmware.config.section.CommitConfig;
import com.vmware.config.section.CommitStatsConfig;
import com.vmware.config.section.GitRepoConfig;
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
import com.vmware.util.scm.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAction implements Action {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected final GitRepoConfig gitRepoConfig;
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

    protected ServiceLocator serviceLocator;

    protected Git git;

    protected boolean failIfCannotBeRun;

    private String[] expectedCommandsToBeAvailable;


    public BaseAction(WorkflowConfig config) {
        this.config = config;
        this.commitConfig = config.commitConfig;
        this.statsConfig = config.statsConfig;
        this.gitRepoConfig = config.gitRepoConfig;
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
    }

    public void checkIfWorkflowShouldBeFailed() {
        checkExpectedCommands();
        if (failIfCannotBeRun) {
            String cannotBeRunReason = this.cannotRunAction();
            if (StringUtils.isNotEmpty(cannotBeRunReason)) {
                exitDueToFailureCheck(cannotBeRunReason);
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
    public String cannotRunAction() {
        return null;
    }

    @Override
    public void preprocess() {
    }

    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        this.git = serviceLocator.getGit();
    }

    protected void setExpectedCommandsToBeAvailable(String... commands) {
        this.expectedCommandsToBeAvailable = commands;
    }

    protected void exitWithMessage(String message) {
        log.info("");
        log.info("Exiting as {}", message);
        System.exit(0);
    }

    protected void exitWithErrorMessage(String message) {
        log.info("");
        log.error(message);
        System.exit(1);
    }

    protected void exitWithWarnMessage(String message) {
        log.info("");
        log.warn(message);
        System.exit(0);
    }

    private void checkExpectedCommands() {
        if (expectedCommandsToBeAvailable == null) {
            return;
        }
        for (String command : expectedCommandsToBeAvailable) {
            if (!CommandLineUtils.isCommandAvailable(command)) {
                exitDueToFailureCheck("command " + command + " is not available");
            }
        }
    }

    protected void exitDueToFailureCheck(String reason) {
        exitWithErrorMessage("Workflow failed by action " + this.getClass().getSimpleName() + " as " + reason);
    }

}
