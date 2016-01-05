package com.vmware.action.bugzilla;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Executes the named query for loading Bugzilla bugs. Only executed if the user has a matching named query.")
public class PreloadAssignedBugzillaBugs extends AbstractCommitAction {

    private Bugzilla bugzilla;

    public PreloadAssignedBugzillaBugs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Runnable loadJiraIssues = new Runnable() {
            @Override
            public void run() {
                try {
                    bugzilla = serviceLocator.getUnauthenticatedBugzilla();
                    List<String> queries = bugzilla.getSavedQueries();
                    log.debug("Bugzilla queries for user {}, {}", config.username, queries.toString());
                    if (!queries.contains(config.bugzillaQuery)) {
                        return;
                    }
                    draft.userHasBugzillaQuery = true;
                    if (bugzilla.isConnectionAuthenticated()) {
                        draft.isPreloadingBugzillaBugs = true;
                        draft.addBugs(bugzilla.getBugsForQuery(config.bugzillaQuery));
                        draft.isPreloadingBugzillaBugs = false;
                    }
                } catch (IOException | URISyntaxException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(loadJiraIssues).start();
    }
}
