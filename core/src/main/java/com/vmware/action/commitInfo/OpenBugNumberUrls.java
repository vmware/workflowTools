package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.SystemUtils;

@ActionDescription("Opens the urls for the bug numbers in the commit.")
public class OpenBugNumberUrls extends BaseCommitAction {
    public OpenBugNumberUrls(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(StringUtils.isEmpty(draft.bugNumbers), "no bug numbers set");
    }

    @Override
    public void process() {
        String[] bugNumbers = draft.bugNumbers.split(",");
        log.info("Opening urls for bug number{} {}", bugNumbers.length > 1 ? "s" : "", draft.bugNumbers);
        for (String bugNumber : bugNumbers) {
            bugNumber = bugNumber.trim();
            Integer bugzillaNumber = bugzillaConfig.parseBugzillaBugNumber(bugNumber);
            if (bugzillaNumber != null) {
                log.debug("Assuming bug {} is a bugzilla bug as it is was parseable as a bugzilla bug number", bugNumber);
                SystemUtils.openUrl(bugzillaConfig.bugzillaUrl(bugzillaNumber));
            } else {
                log.debug("Assuming bug {} is JIRA issue as it did not parse as a bugzilla bug number", bugNumber);
                SystemUtils.openUrl(jiraConfig.issueUrl(bugNumber));
            }
        }
    }
}
