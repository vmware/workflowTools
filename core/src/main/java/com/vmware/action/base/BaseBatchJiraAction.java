package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;

public abstract class BaseBatchJiraAction extends BaseIssuesProcessingAction {

    protected Jira jira;

    public BaseBatchJiraAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(jiraConfig.disableJira, "Jira is disabled by config property disableJira");
    }

    @Override
    public void asyncSetup() {
        this.jira = serviceLocator.getJira();
    }

    @Override
    public void preprocess() {
        this.jira.setupAuthenticatedConnection();
    }
}
