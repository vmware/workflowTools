package com.vmware.action.bugzilla;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Executes the named query for loading Bugzilla bugs. Only executed if the user has a matching named query.")
public class PreloadAssignedBugzillaBugs extends AbstractCommitAction {

    private Bugzilla bugzilla;

    public PreloadAssignedBugzillaBugs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        Runnable loadBugzillaBugs = new Runnable() {
            @Override
            public void run() {
                try {
                    bugzilla = serviceLocator.getUnauthenticatedBugzilla();
                    if (bugzilla.isBaseUriTrusted() && bugzilla.isConnectionAuthenticated()) {
                        draft.isPreloadingBugzillaBugs = true;
                        if (bugzilla.containsSavedQuery(config.bugzillaQuery)) {
                            draft.userHasBugzillaQuery = true;
                            draft.addBugs(bugzilla.getBugsForQuery(config.bugzillaQuery));
                        }
                        draft.isPreloadingBugzillaBugs = false;
                    }
                } catch (IOException | URISyntaxException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(loadBugzillaBugs).start();
    }
}
