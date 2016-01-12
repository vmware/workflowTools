package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Resolves bugzilla bugs with a status of fixed.")
public class MarkBugAsResolved extends BaseCommitAction {

    protected Bugzilla bugzilla;

    public MarkBugAsResolved(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        this.bugzilla = serviceLocator.getAuthenticatedBugzilla();
    }

    @Override
    public String cannotRunAction() {
        if (draft.hasBugNumber(config.noBugNumberLabel)) {
            return super.cannotRunAction();
        }
        return "commit has no bug number";
    }


    @Override
    public void process() {
        for (String bugNumber : draft.bugNumbersAsArray()) {
            Integer bugId = config.parseBugzillaBugNumber(bugNumber.trim());
            if (bugId == null) {
                log.info("{} is not a Bugzilla id, assuming that it is a Jira issue key, skipping", bugNumber);
                return;
            }
            bugzilla.resolveBug(bugId, BugResolutionType.Fixed);
        }
    }
}
