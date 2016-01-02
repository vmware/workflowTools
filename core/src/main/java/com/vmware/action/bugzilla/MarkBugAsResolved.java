package com.vmware.action.bugzilla;

import com.vmware.ServiceLocator;
import com.vmware.action.base.AbstractCommitAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Resolves bugzilla bugs with a status of fixed.")
public class MarkBugAsResolved extends AbstractCommitAction {

    protected Bugzilla bugzilla;

    public MarkBugAsResolved(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.bugzilla = ServiceLocator.getBugzilla(config.bugzillaUrl, config.username, config.bugzillaTestBug, true);
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
        if (StringUtils.isBlank(draft.bugNumbers)) {
            log.info("No bug numbers found in commit");
            return;
        }

        for (String bugNumber : draft.bugNumbers.split(",")) {
            Integer bugId = config.parseBugzillaBugNumber(bugNumber.trim());
            if (bugId == null) {
                log.info("{} is not a bugzilla id, assuming that it is a jira issue key, skipping", bugNumber);
                return;
            }
            bugzilla.resolveBug(bugId, BugResolutionType.Fixed);
        }
    }
}
