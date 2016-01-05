package com.vmware;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.trello.Trello;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Centralized locator for jenkins, jira, reviewboard and trello instances.
 * Ensures that the above classes are singletons.
 */
public class ServiceLocator {

    private Jira jira;

    private Bugzilla bugzilla;

    private ReviewBoard reviewBoard;

    private Jenkins jenkins;

    private Trello trello;

    private WorkflowConfig config;

    public ServiceLocator(WorkflowConfig config) {
        this.config = config;
    }

    public Jira getUnauthenticatedJira() throws IllegalAccessException, IOException, URISyntaxException {
        if (jira == null) {
            jira = new Jira(config.jiraUrl, config.jiraTestIssue);
        }
        return jira;
    }

    public Jira getAuthenticatedJira() throws IllegalAccessException, IOException, URISyntaxException {
        if (jira == null) {
            jira = new Jira(config.jiraUrl, config.jiraTestIssue);
        }
        jira.setupAuthenticatedConnection();
        return jira;
    }

    public Bugzilla getUnauthenticatedBugzilla() throws IllegalAccessException, IOException, URISyntaxException {
        if (bugzilla == null) {
            bugzilla = new Bugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug);
        }
        return bugzilla;
    }

    public Bugzilla getAuthenticatedBugzilla() throws IllegalAccessException, IOException, URISyntaxException {
        if (bugzilla == null) {
            bugzilla = new Bugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug);
        }
        bugzilla.setupAuthenticatedConnection();
        return bugzilla;
    }

    public ReviewBoard getReviewBoard() throws IOException, URISyntaxException, IllegalAccessException {
        if (reviewBoard == null) {
            reviewBoard = new ReviewBoard(config.reviewboardUrl, config.username);
            reviewBoard.setupAuthenticatedConnection();
            reviewBoard.updateServerTimeZone(config.reviewBoardDateFormat);
        }
        return reviewBoard;
    }

    public Jenkins getJenkins() throws IOException, URISyntaxException, IllegalAccessException {
        if (jenkins == null) {
            jenkins = new Jenkins(config.jenkinsUrl, config.username, config.jenkinsUsesCsrf, config.disableJenkinsLogin);
            jenkins.setupAuthenticatedConnection();
        }
        return jenkins;
    }

    public Trello getTrello() throws IOException, URISyntaxException, IllegalAccessException {
        if (trello == null) {
            trello = new Trello(config.trelloUrl);
            trello.setupAuthenticatedConnection();
        }
        return trello;
    }

}
