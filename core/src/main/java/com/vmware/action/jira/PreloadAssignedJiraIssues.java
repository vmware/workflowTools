package com.vmware.action.jira;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.ServiceLocator;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@ActionDescription("Preloads the list of assigned jira issues. Loads asynchronously so that the list is already available when editing the bug number")
public class PreloadAssignedJiraIssues extends AbstractCommitAction {

    private Jira jira;

    public PreloadAssignedJiraIssues(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Runnable loadJiraIssues = new Runnable() {
            @Override
            public void run() {
                try {
                    jira = ServiceLocator.getJira(config.jiraUrl, config.jiraTestIssue, false);
                    if (jira.isConnectionAuthenticated()) {
                        draft.isPreloadingJiraIssues = true;
                        draft.addIssues(jira.getOpenTasksForUser(config.username).issues);
                        draft.isPreloadingJiraIssues = false;
                    }
                } catch (IOException | URISyntaxException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(loadJiraIssues).start();
    }
}
