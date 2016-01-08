package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseBatchJiraAction extends BaseMultiActionDataSupport {

    protected Jira jira;

    public BaseBatchJiraAction(WorkflowConfig config) {
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
