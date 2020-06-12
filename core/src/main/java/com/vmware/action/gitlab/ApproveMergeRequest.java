package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.util.exception.FatalException;

@ActionDescription("Approve merge request in Gitlab.")
public class ApproveMergeRequest extends BaseCommitWithMergeRequestAction {
    public ApproveMergeRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(draft.mergeRequestProjectId(), draft.mergeRequestId());
        if (approvals.approvalsRequired != null && approvals.approvalsRequired == 0) {
            log.info("No approvals required for merge request {}", draft.mergeRequestUrl);
            return;
        }

        if (approvals.userHasApproved) {
            log.info("Merge request {} has already been self approved", draft.mergeRequestUrl);
            return;
        }

        if (!approvals.userCanApprove) {
            throw new FatalException("Merge request {} cannot be self approved", draft.mergeRequestUrl);
        }

        log.info("Self approving merge request {}", draft.mergeRequestUrl);
        MergeRequestApprovals updatedApprovals = gitlab.approveMergeRequest(draft.mergeRequestProjectId(), draft.mergeRequestId());
        if (!updatedApprovals.userHasApproved) {
            throw new FatalException("Merge request {} is not self approved", draft.mergeRequestUrl);
        }
    }
}
