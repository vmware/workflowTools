package com.vmware;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.buildweb.Buildweb;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.scm.Git;
import com.vmware.scm.Perforce;
import com.vmware.trello.Trello;

/**
 * Centralized locator for git, perforce, jenkins, jira, reviewboard and trello instances.
 * Ensures that the above classes are singletons.
 */
public class ServiceLocator {

    private Git git;

    private Perforce perforce;

    private Jira jira;

    private Bugzilla bugzilla;

    private ReviewBoard reviewBoard;

    private Jenkins jenkins;

    private Buildweb buildweb;

    private Trello trello;

    private WorkflowConfig config;

    public ServiceLocator(WorkflowConfig config) {
        this.config = config;
    }

    public Jira getUnauthenticatedJira() {
        if (jira == null) {
            jira = new Jira(config.jiraUrl, config.jiraTestIssue);
        }
        return jira;
    }

    public Jira getAuthenticatedJira() {
        if (jira == null) {
            jira = new Jira(config.jiraUrl, config.jiraTestIssue);
        }
        jira.setupAuthenticatedConnection();
        return jira;
    }

    public Bugzilla getUnauthenticatedBugzilla() {
        if (bugzilla == null) {
            bugzilla = new Bugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug);
        }
        return bugzilla;
    }

    public Bugzilla getAuthenticatedBugzilla() {
        if (bugzilla == null) {
            bugzilla = new Bugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug);
        }
        bugzilla.setupAuthenticatedConnection();
        return bugzilla;
    }

    public ReviewBoard getReviewBoard() {
        if (reviewBoard == null) {
            reviewBoard = new ReviewBoard(config.reviewboardUrl, config.username);
            reviewBoard.setupAuthenticatedConnection();
            reviewBoard.updateServerTimeZone(config.reviewBoardDateFormat);
        }
        return reviewBoard;
    }

    public Jenkins getJenkins() {
        if (jenkins == null) {
            jenkins = new Jenkins(config.jenkinsUrl, config.username, config.jenkinsUsesCsrf, config.disableJenkinsLogin);
            jenkins.setupAuthenticatedConnection();
        }
        return jenkins;
    }

    public Buildweb getBuildweb() {
        if (buildweb == null) {
            buildweb = new Buildweb(config.buildwebApiUrl, config.username);
        }
        return buildweb;
    }

    public Trello getTrello() {
        if (trello == null) {
            trello = new Trello(config.trelloUrl);
            trello.setupAuthenticatedConnection();
        }
        return trello;
    }

    public Git getGit() {
        if (git == null) {
            git = new Git();
        }
        return git;
    }

    public Perforce getPerforce() {
        if (perforce == null) {
            perforce = new Perforce(config.perforceClientName);
        }
        return perforce;
    }
}
