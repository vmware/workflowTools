package com.vmware.action.bugzilla;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

/**
 * Executes the named query for loading bugzilla bugs. Only executed if the user has a matching named query.
 */
public class PreloadAssignedBugzillaBugs extends AbstractCommitAction {

    private Bugzilla bugzilla;

    public PreloadAssignedBugzillaBugs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        bugzilla = ServiceLocator.getBugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug, false);
        List<String> queries = bugzilla.getSavedQueries();
        if (!queries.contains(config.bugzillaQuery)) {
            log.info("Can't load your bugzilla bug list as saved query {} not found in your bugzilla query list," +
                    "\nPlease create if you want to select easily select the bugzilla bug number", config.bugzillaQuery);
            log.debug("Bugzilla queries for user {}, {}", config.username, queries.toString());
            return;
        }

        Runnable loadJiraIssues = new Runnable() {
            @Override
            public void run() {
                try {
                    if (bugzilla.isConnectionAuthenticated()) {
                        draft.isPreloadingBugzillaBugs = true;
                        draft.addBugs(bugzilla.getBugsForQuery(config.bugzillaQuery));
                        draft.isPreloadingBugzillaBugs = false;
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(loadJiraIssues).start();
    }
}
