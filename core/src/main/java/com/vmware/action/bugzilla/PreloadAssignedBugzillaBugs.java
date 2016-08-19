package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Executes the named query for loading Bugzilla bugs. Only executed if the user has a matching named query.")
public class PreloadAssignedBugzillaBugs extends BaseCommitAction {

    private Bugzilla bugzilla;

    public PreloadAssignedBugzillaBugs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Runnable loadBugzillaBugs = new Runnable() {
            @Override
            public void run() {
                bugzilla = serviceLocator.getBugzilla();
                if (bugzilla.isBaseUriTrusted() && bugzilla.isConnectionAuthenticated()) {
                    draft.isPreloadingBugzillaBugs = true;
                    if (bugzilla.containsSavedQuery(config.bugzillaQuery)) {
                        draft.userHasBugzillaQuery = true;
                        draft.addBugs(bugzilla.getBugsForQuery(config.bugzillaQuery));
                    }
                    draft.isPreloadingBugzillaBugs = false;
                }
            }
        };
        new Thread(loadBugzillaBugs).start();
    }
}
