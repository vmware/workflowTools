package com.vmware.action.base;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueTypeDefinition;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseBatchBugzillaAction extends BaseMultiActionDataSupport {

    protected Bugzilla bugzilla;

    public BaseBatchBugzillaAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.bugzilla = serviceLocator.getAuthenticatedBugzilla();
    }

    @Override
    public String cannotRunAction() {
        if (config.disableBugzilla) {
            return "Bugzilla is disabled by config property disableBugzilla";
        }
        return super.cannotRunAction();
    }

    protected Issue createIssueFromBug(Bug bug) {
        String summary = "[BZ-" + bug.getKey() + "] " + bug.getSummary();
        String description = bug.getWebUrl() + "\n" + bug.getDescription();
        Issue matchingIssue = new Issue(IssueTypeDefinition.Bug, config.defaultJiraProject,
                config.defaultJiraComponent, summary, description, null);
        return matchingIssue;
    }
}
