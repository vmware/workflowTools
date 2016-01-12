package com.vmware.action.jira;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Preloads the list of assigned jira issues. Loads asynchronously so that the list is already available when editing the bug number.")
public class PreloadAssignedJiraIssues extends BaseCommitAction {

    private Jira jira;

    public PreloadAssignedJiraIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Runnable loadJiraIssues = new Runnable() {
            @Override
            public void run() {
                jira = serviceLocator.getUnauthenticatedJira();
                if (jira.isBaseUriTrusted() && jira.isConnectionAuthenticated()) {
                    draft.isPreloadingJiraIssues = true;
                    draft.addIssues(jira.getOpenTasksForUser(config.username).issues);
                    draft.isPreloadingJiraIssues = false;
                }
            }
        };
        new Thread(loadJiraIssues).start();
    }
}
