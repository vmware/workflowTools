package com.vmware.action.bugzilla;

import com.vmware.action.base.BaseBatchBugzillaAction;
import com.vmware.bugzilla.domain.Bug;
import com.vmware.bugzilla.domain.BugResolutionType;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.Jira;
import com.vmware.jira.domain.Issue;
import com.vmware.jira.domain.IssueResolutionDefinition;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Resolves Bugzilla bugs whose tracking Jira issues are resolved.")
public class ResolveBugsWithResolvedTrackingIssues extends BaseBatchBugzillaAction {

    protected Jira jira;

    public ResolveBugsWithResolvedTrackingIssues(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        super.preprocess();
        this.jira = serviceLocator.getAuthenticatedJira();
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (config.disableJira) {
            log.warn("Jira is disabled by config property disableJira");
            return false;
        }

        if (multiActionData.noBugsAdded()) {
            log.info("No bugs added");
            return false;
        }

        return super.canRunAction();
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        for (Bug bug : multiActionData.getBugsForProcessing()) {
            Integer bugId = Integer.parseInt(bug.getKey());
            Issue trackingIssue = getValidTrackingIssueForBug(bug);

            if (trackingIssue == null) {
                continue;
            }

            IssueResolutionDefinition resolutionDefinition = trackingIssue.getResolution();
            resolveBug(bugId, resolutionDefinition);
        }
    }

    private void resolveBug(Integer bugId, IssueResolutionDefinition resolutionDefinition) throws IllegalAccessException, IOException, URISyntaxException {
        BugResolutionType resolutionType;
        try {
            resolutionType = BugResolutionType.valueOf(resolutionDefinition.name());
        } catch (IllegalArgumentException iae) {
            log.warn("No matching bugzilla resolution for Jira resolution {}, defaulting to fixed resolution",
                    resolutionDefinition.name());
            resolutionType = BugResolutionType.Fixed;
        }

        bugzilla.resolveBug(bugId, resolutionType);
    }

    private Issue getValidTrackingIssueForBug(Bug bug) throws IOException, URISyntaxException {
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
