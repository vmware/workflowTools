package com.vmware.action.base;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jira.IssueTypeDefinition;
import com.vmware.jira.domain.Issue;

public abstract class BaseBatchBugzillaAction extends BaseIssuesProcessingAction {

    protected Bugzilla bugzilla;

    public BaseBatchBugzillaAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        this.bugzilla = serviceLocator.getBugzilla();
    }

    @Override
    public void preprocess() {
        this.bugzilla.setupAuthenticatedConnection();
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(bugzillaConfig.disableBugzilla, "Bugzilla is disabled by config property disableBugzilla");
    }

    protected Issue createIssueFromBug(Bug bug) {
        String summary = "[BZ-" + bug.getKey() + "] " + bug.getSummary();
        String description = bug.getWebUrl() + "\n" + bug.getDescription();
        return new Issue(IssueTypeDefinition.Bug, jiraConfig.defaultJiraProject,
                jiraConfig.defaultJiraComponent, summary, description, null);
    }
}
