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
import com.vmware.config.section.TrelloConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.NoPerforceClientForDirectoryException;
import com.vmware.util.scm.Perforce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseAction {

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
    }

    /**
     * @return Reason why the workflow should fail, null if it should continue
     */
    public String failWorkflowIfConditionNotMet() {
        if (expectedCommandsToBeAvailable == null) {
            return null;
        }
        for (String command : expectedCommandsToBeAvailable) {
            if (!CommandLineUtils.isCommandAvailable(command)) {
                return "command " + command + " is not available";
            }
        }
        if (failIfCannotBeRun) {
            String cannotBeRunReason = this.cannotRunAction();
            if (StringUtils.isNotBlank(cannotBeRunReason)) {
                return cannotBeRunReason;
            }
        }
        return null;
    }

    protected Perforce getLoggedInPerforceClient() {
        String reasonForFailing = perforceClientCannotBeUsed();
        if (StringUtils.isNotBlank(reasonForFailing)) {
            throw new FatalException("Exiting as " + reasonForFailing);
        }
        return serviceLocator.getPerforce();
    }

    protected String perforceClientCannotBeUsed() {
        if (!CommandLineUtils.isCommandAvailable("p4")) {
            return "p4 command is not available";
        }
        Perforce perforce = serviceLocator.getPerforce();
        if (!perforce.isLoggedIn()) {
            return "perforce user is not logged in";
        }
        if (StringUtils.isBlank(perforceClientConfig.perforceClientName)) {
            try {
                perforceClientConfig.perforceClientName = perforce.getClientName();
            } catch (NoPerforceClientForDirectoryException npc) {
                return npc.getMessage();
            }
        }
        return null;
    }

    /**
     * Setup method that will run asynchonrously, useful for setting up rest services
     */
    public void asyncSetup() {
    }

    /**
     * @return Reason for why this action should not be run, null if it should be run
     */
    public String cannotRunAction() {
        return null;
    }

    /**
     * Override if any setup is needed before the process method is called
     */
    public void preprocess() {
    }

    public void setServiceLocator(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        this.git = serviceLocator.getGit();
    }

    public abstract void process();

    protected void setExpectedCommandsToBeAvailable(String... commands) {
        this.expectedCommandsToBeAvailable = commands;
    }

}
