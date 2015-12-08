package com.vmware.action.bugzilla;

import com.vmware.action.base.AbstractBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Adds tracking issues for bugs in a Bugzilla named query. Bugs that already have a tracking issue are skipped.")
public class AddTrackingIssuesForQuery extends AbstractBatchBugzillaAction {

    public AddTrackingIssuesForQuery(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        log.info("Using named query {} for retrieving bugs, change by specifying --bugzilla-query=QueryName", config.bugzillaQuery);
        List<Bug> bugList = bugzilla.getBugsForQuery(config.bugzillaQuery);
        if (bugList.isEmpty()) {
            log.info("No bugs found for named query {}", config.bugzillaQuery);
            return;
        }
        for (Bug bug : bugList) {
            String trackingIssueKey = bug.getTrackingIssueKey();
            if (trackingIssueKey != null) {
                log.info("Bug {} is already being tracked by issue {}, ignoring", bug.getKey(), trackingIssueKey);
                continue;
            }
            Issue trackingIssue = createIssueFromBug(bug);
            projectIssues.add(trackingIssue);
            log.info("\nA Jira Issue will be created in Jira Project {} to track bug {}\n{}", config.defaultJiraProject,
                    trackingIssue.matchingBugzillaNumber(config.bugzillaUrl), bug.getSummary());
        }

        if (projectIssues.isEmpty()) {
            log.info("No issues added", config.bugzillaQuery);
        }
    }
}
