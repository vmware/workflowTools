package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.util.exception.FatalException;

@ActionDescription("Approve pull request in Github.")
public class ApprovePullRequest extends BaseCommitWithMergeRequestAction {
    public ApprovePullRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(gitlabConfig.gitlabProjectId, draft.mergeRequestId());
        if (approvals.approvalsRequired != null && approvals.approvalsRequired == 0) {
            log.info("No approvals required for pull request {}", draft.requestUrl);
            return;
        }

        if (approvals.userHasApproved) {
            log.info("Pull request {} has already been self approved", draft.requestUrl);
            return;
        }

        if (!approvals.userCanApprove) {
            throw new FatalException("Pull request {} cannot be self approved", draft.requestUrl);
        }

        log.info("Self approving pull request {}", draft.requestUrl);
        MergeRequestApprovals updatedApprovals = gitlab.approveMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId());
        if (!updatedApprovals.userHasApproved) {
            throw new FatalException("Pull request {} is not self approved", draft.requestUrl);
        }
        draft.setGitlabMergeRequest(gitlab.getMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId()));
    }
}
