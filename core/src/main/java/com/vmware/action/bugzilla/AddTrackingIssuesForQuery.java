package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;

import java.util.List;

@ActionDescription("Adds tracking issues for bugs in a Bugzilla named query. Bugs that already have a tracking issue are skipped.")
public class AddTrackingIssuesForQuery extends BaseBatchBugzillaAction {

    public AddTrackingIssuesForQuery(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        List<Bug> bugList = projectIssues.getBugsForProcessing();
        super.skipActionIfTrue(bugList.isEmpty(), "no bugs found for named query " + bugzillaConfig.bugzillaQuery);
    }

    @Override
    public void process() {
        List<Bug> bugList = projectIssues.getBugsForProcessing();
        for (Bug bug : bugList) {
            String trackingIssueKey = bug.getTrackingIssueKey();
            if (trackingIssueKey != null) {
                log.info("Bug {} is already being tracked by issue {}, ignoring", bug.getKey(), trackingIssueKey);
                continue;
            }
            Issue trackingIssue = createIssueFromBug(bug);
            projectIssues.add(trackingIssue);
            log.info("\nA Jira Issue will be created in Jira Project {} to track bug {}\n{}", jiraConfig.defaultJiraProject,
                    trackingIssue.matchingBugzillaNumber(bugzillaConfig.bugzillaUrl), bug.getSummary());
        }

        if (projectIssues.noIssuesAdded()) {
            log.info("No issues added for query {}", bugzillaConfig.bugzillaQuery);
        }
    }
}
