package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueResolutionDefinition;

@ActionDescription("Resolves Bugzilla bugs whose tracking Jira issues are resolved.")
public class ResolveBugsWithResolvedTrackingIssues extends BaseBatchBugzillaAction {

    protected Jira jira;

    public ResolveBugsWithResolvedTrackingIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        super.asyncSetup();
        this.jira = serviceLocator.getJira();
    }

    @Override
    public void preprocess() {
        super.preprocess();
        this.jira.setupAuthenticatedConnection();
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(jiraConfig.disableJira, "Jira is disabled by config property disableJira");
        super.skipActionIfTrue(projectIssues.noBugsAdded(), "no bugs added");
    }

    @Override
    public void process() {
        for (Bug bug : projectIssues.getBugsForProcessing()) {
            Integer bugId = Integer.parseInt(bug.getKey());
            Issue trackingIssue = getValidTrackingIssueForBug(bug);

            if (trackingIssue == null) {
                continue;
            }

            BugResolutionType resolutionType = determinBugResolutionForIssueResolution(trackingIssue.getResolution());
            bugzilla.resolveBug(bugId, resolutionType);
        }
    }

    private BugResolutionType determinBugResolutionForIssueResolution(IssueResolutionDefinition resolutionDefinition) {
        BugResolutionType resolutionType;
        try {
            resolutionType = BugResolutionType.valueOf(resolutionDefinition.name());
        } catch (IllegalArgumentException iae) {
            log.warn("No matching bugzilla resolution for Jira resolution {}, defaulting to {} resolution",
                    resolutionDefinition.name(), BugResolutionType.Fixed.name());
            resolutionType = BugResolutionType.Fixed;
        }
        return resolutionType;
    }

    private Issue getValidTrackingIssueForBug(Bug bug) {
        Integer bugId = Integer.parseInt(bug.getKey());
        if (bug.resolution != null) {
            log.info("Bug {} already has a resolution of {}, skipping", bugId, bug.resolution.getValue());
            return null;
        }

        String trackingIssueKey = bug.getTrackingIssueKey();
        if (trackingIssueKey == null) {
            log.info("No Jira tracking issue found for bug {}, skipping", bugId);
            return null;
        }

        Issue trackingIssue = jira.getIssueWithoutException(trackingIssueKey);
        if (trackingIssue.isNotFound) {
            log.info("Tracking issue {} for bug {} does not exist, skipping", trackingIssueKey, bugId);
            return null;
        }

        if (trackingIssue.getResolution() == null) {
            log.info("Tracking issue {} for bug {} has not been resolved, skipping", trackingIssueKey, bugId);
            return null;
        }
        return trackingIssue;
    }
}
