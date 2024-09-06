package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovals;

@ActionDescription("Checks the status of approvals for a merge request")
public class CheckStatusOfMergeRequestApprovals extends BaseCommitWithMergeRequestAction {
    public CheckStatusOfMergeRequestApprovals(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(mergeRequest.projectId, mergeRequest.iid);
        log.info(approvals.approvalInfo());
    }
}
