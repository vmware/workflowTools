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
        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(gitlabConfig.gitlabProjectId, draft.mergeRequestId());
        if (approvals.approvalsRequired != null && approvals.approvalsRequired == 0) {
            log.info("No approvals required for merge request {}", draft.requestUrl);
            return;
        }

        if (approvals.userHasApproved) {
            log.info("Merge request {} has already been self approved", draft.requestUrl);
            return;
        }

        if (!approvals.userCanApprove) {
            throw new FatalException("Merge request {} cannot be self approved", draft.requestUrl);
        }

        log.info("Self approving merge request {}", draft.requestUrl);
        MergeRequestApprovals updatedApprovals = gitlab.approveMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId());
        if (!updatedApprovals.userHasApproved) {
            throw new FatalException("Merge request {} is not self approved", draft.requestUrl);
        }
        draft.setGitlabMergeRequest(gitlab.getMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId()));
    }
}
