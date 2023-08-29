package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@ActionDescription("Exists if the merge request does not have all required approvals")
public class ExitIfMergeRequestDoesNotHaveRequiredApprovals extends BaseCommitWithMergeRequestAction {
    public ExitIfMergeRequestDoesNotHaveRequiredApprovals(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(StringUtils.isLong(draft.id), "reviewboard request " + draft.id + " is associated with this commit");
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(draft.reviewedBy) || commitConfig.trivialReviewerLabel.equals(draft.reviewedBy)) {
            return;
        }
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(mergeRequest.projectId, mergeRequest.iid);
        if (approvals.approvalsLeft != null && (approvals.approvalsLeft > 1
                || (approvals.approvalsLeft == 1 && (approvals.userHasApproved || !approvals.userCanApprove)))) {
            cancelWithMessage(approvals.approvalInfo());
        } else {
            draft.shipItReviewers = approvals.approvedBy != null ? Arrays.stream(approvals.approvedBy)
                    .map(approvalUser -> approvalUser.user.username).collect(Collectors.joining(",")) : "";
            log.info(approvals.approvalInfo());
        }
    }
}
