package com.vmware.action.base;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueTypeDefinition;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class AbstractBatchBugzillaAction extends AbstractBatchIssuesAction {

    protected Bugzilla bugzilla;

    public AbstractBatchBugzillaAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.bugzilla = serviceLocator.getAuthenticatedBugzilla();
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (config.disableBugzilla) {
            log.warn("Bugzilla is disabled by config property disableBugzilla");
            return false;
        }
        return super.canRunAction();
    }

    protected Issue createIssueFromBug(Bug bug) {
        String summary = "[BZ-" + bug.getKey() + "] " + bug.getSummary();
        String description = bug.getWebUrl() + "\n" + bug.getDescription();
        Issue matchingIssue = new Issue(IssueTypeDefinition.Bug, config.defaultJiraProject,
                config.defaultJiraComponent, summary, description, null);
        return matchingIssue;
    }
}
