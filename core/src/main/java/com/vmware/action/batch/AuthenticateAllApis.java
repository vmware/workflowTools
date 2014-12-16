package com.vmware.action.batch;

import com.vmware.AbstractRestService;
import com.vmware.action.AbstractAction;
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
public class AuthenticateAllApis extends AbstractAction{

    public AuthenticateAllApis(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        checkAuthentication(new Trello(config.trelloUrl));
        checkAuthentication(new Jira(config.jiraUrl));
        checkAuthentication(new ReviewBoard(config.reviewboardUrl, config.username));
        checkAuthentication(new Jenkins(config.jenkinsUrl, config.username, config.jenkinsUsesCsrf));
    }

    private void checkAuthentication(AbstractRestService restService) throws IOException, URISyntaxException, IllegalAccessException {
        String serviceName = restService.getClass().getSimpleName();
        log.info("Checking authentication for service {}", serviceName);
        restService.setupAuthenticatedConnection();
        log.info("Finished checking authentication for service {}", serviceName);
    }
}
