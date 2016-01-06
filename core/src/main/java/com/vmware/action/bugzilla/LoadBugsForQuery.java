package com.vmware.action.bugzilla;

import com.vmware.action.base.AbstractBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Adds bugs for a named query in Bugzilla.")
public class LoadBugsForQuery extends AbstractBatchBugzillaAction {

    public LoadBugsForQuery(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        log.info("Using named query {} for retrieving bugs, change by specifying --bugzilla-query=QueryName", config.bugzillaQuery);
        List<Bug> bugList = bugzilla.getBugsForQuery(config.bugzillaQuery);
        multiActionData.addAllBugs(bugList);
    }
}
