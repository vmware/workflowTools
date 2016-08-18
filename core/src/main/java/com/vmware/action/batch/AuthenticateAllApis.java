package com.vmware.action.batch;

import com.vmware.AbstractService;
import com.vmware.action.BaseAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.trello.Trello;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Ensures that all apis have a valid token / cookie. Primarily for testing.")
public class AuthenticateAllApis extends BaseAction {

    public AuthenticateAllApis(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        checkAuthentication(new Trello(config.trelloUrl));
        checkAuthentication(new Bugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug));
        checkAuthentication(new Jira(config.jiraUrl, config.jiraTestIssue, config.username));
        checkAuthentication(new ReviewBoard(config.reviewboardUrl, config.username));
        checkAuthentication(new Jenkins(config.jenkinsUrl, config.username, config.jenkinsUsesCsrf, config.disableJenkinsLogin));
    }

    private void checkAuthentication(AbstractService restService) {
        String serviceName = restService.getClass().getSimpleName();
        log.info("Checking authentication for service {}", serviceName);
        restService.setupAuthenticatedConnection();
        log.info("Finished checking authentication for service {}", serviceName);
    }
}
