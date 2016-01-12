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
    public String cannotRunAction() {
        if (config.disableJira) {
            return "Jira is disabled by config property disableJira";
        }
        return super.cannotRunAction();
    }

    @Override
    public void preprocess() {
        this.jira = serviceLocator.getAuthenticatedJira();
    }
}
