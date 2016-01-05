package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class AbstractBatchJiraAction extends AbstractBatchIssuesAction {

    protected Jira jira;

    public AbstractBatchJiraAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (config.disableJira) {
            log.warn("Jira is disabled by config property disableJira");
            return false;
        }
        return super.canRunAction();
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.jira = serviceLocator.getAuthenticatedJira();
    }
}
