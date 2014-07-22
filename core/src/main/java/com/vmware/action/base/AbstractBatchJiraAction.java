package com.vmware.action.base;

import com.vmware.ServiceLocator;
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
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.jira = ServiceLocator.getJira(config.jiraUrl, true);
    }
}
