package com.vmware.action.jira;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.ServiceLocator;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Preloads the list of assigned jira issues. Loads asynchronously so that the list is already available when editing the bug number")
public class PreloadAssignedIssues extends AbstractCommitAction {

    private Jira jira;

    public PreloadAssignedIssues(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Runnable loadJiraIssues = new Runnable() {
            @Override
            public void run() {
                try {
                    jira = ServiceLocator.getJira(config.jiraUrl, false);
                    if (jira.isConnectionAuthenticated()) {
                        draft.isPreloadingJiraIssues = true;
                        draft.openJiraIssues = jira.getOpenTasksForUser(config.username).issues;
                        draft.isPreloadingJiraIssues = false;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(loadJiraIssues).start();
    }
}
