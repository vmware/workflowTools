package com.vmware.action.git;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Runs git p4 --prepare-p4-only, then moves changes to the specified changelist.")
public class AddGitChangesToChangelist extends BasePerforceCommitAction {
    public AddGitChangesToChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Using git p4 to add commit changes diffed against tracking branch {} to default changelist in perforce",
                gitRepoConfig.trackingBranchPath());
        git.addChangesToDefaultChangelist(gitRepoConfig.trackingBranchPath());

        if (StringUtils.isEmpty(draft.perforceChangelistId)) {
            log.warn("No changelist associated with commit, leaving changes in default changelist");
            return;
        }

        log.info("Moving changes to changelist {}", draft.perforceChangelistId);
        perforce.reopenAllOpenFilesInChangelist(draft.perforceChangelistId);
    }
}
