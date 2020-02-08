package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Resolves bugzilla bugs with a status of fixed.")
public class MarkBugAsResolved extends BaseCommitAction {

    protected Bugzilla bugzilla;

    public MarkBugAsResolved(WorkflowConfig config) {
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
    public String cannotRunAction() {
        if (!draft.hasBugNumber(commitConfig.noBugNumberLabel)) {
            return "commit has no bug number";
        }
        return super.cannotRunAction();
    }


    @Override
    public void process() {
        for (String bugNumber : draft.bugNumbersAsList()) {
            Integer bugId = bugzillaConfig.parseBugzillaBugNumber(bugNumber);
            if (bugId == null) {
                log.info("{} is not a Bugzilla id, assuming that it is a Jira issue key, skipping", bugNumber);
                return;
            }
            bugzilla.resolveBug(bugId, BugResolutionType.Fixed);
        }
    }
}
