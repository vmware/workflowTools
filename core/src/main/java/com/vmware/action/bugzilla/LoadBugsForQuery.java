package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.List;

@ActionDescription("Adds bugs for a named query in Bugzilla.")
public class LoadBugsForQuery extends BaseBatchBugzillaAction {

    public LoadBugsForQuery(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Using named query {} for retrieving bugs, change by specifying --bugzilla-query=QueryName", bugzillaConfig.bugzillaQuery);
        List<Bug> bugList = bugzilla.getBugsForQuery(bugzillaConfig.bugzillaQuery);
        projectIssues.addAllBugs(bugList);
    }
}
