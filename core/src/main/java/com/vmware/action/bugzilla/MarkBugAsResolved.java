package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

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
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.bugzilla = serviceLocator.getAuthenticatedBugzilla();
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (draft.hasBugNumber(config.noBugNumberLabel)) {
            return true;
        }
        log.info("Skipping action {} as the commit has no bug number", this.getClass().getSimpleName());
        return false;
    }


    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
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
