package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;

import java.util.List;

@ActionDescription("Adds comment to bugs in Bugzilla with the url for the matching issue in Jira.")
public class AddTrackingIssueCreatedComments extends BaseBatchBugzillaAction {

    public AddTrackingIssueCreatedComments(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        List<Issue> issues = projectIssues.getIssuesRepresentingBugzillaBugs(bugzillaConfig.bugzillaUrl);
        skipActionIfTrue(issues.isEmpty(), "no matching issues found");
    }

    @Override
    public void process() {
        List<Issue> issues = projectIssues.getIssuesRepresentingBugzillaBugs(bugzillaConfig.bugzillaUrl);
        for (Issue issue : issues) {
            int bugzillaId = issue.matchingBugzillaNumber(bugzillaConfig.bugzillaUrl);
            Bug matchingBug = bugzilla.getBugByIdWithoutException(bugzillaId);
            if (matchingBug.isNotFound()) {
                log.warn("Bug with id {} was not found in Bugzilla, skipping", bugzillaId);
                continue;
            }

            String matchingJiraIssueComment = Bug.TRACKING_ISSUE_TEXT + issue.getWebUrl();
            if (matchingBug.containsComment(matchingJiraIssueComment)) {
                log.info("A comment has already been added to show tracking issue {}", issue.getKey());
                continue;
            }

            log.info("Adding comment \"{}\" to bug {}", matchingJiraIssueComment, bugzillaId);
            bugzilla.addBugComment(bugzillaId, matchingJiraIssueComment);
        }
    }
}
