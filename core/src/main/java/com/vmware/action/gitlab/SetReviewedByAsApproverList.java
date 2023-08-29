package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.action.base.BaseSetShipItReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

@ActionDescription("Sets the reviewer list for the commit as the list of users who have approved the associated merge request.")
public class SetReviewedByAsApproverList extends BaseCommitWithMergeRequestAction {
    public SetReviewedByAsApproverList(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(StringUtils.isLong(draft.id), "reviewboard request " + draft.id + " is associated with this commit");
    }

    @Override
    public void process() {
        // reuse the result from other actions like ExitIfMergeRequestDoesNotHaveRequiredApprovals
        if (draft.shipItReviewers != null) {
            draft.reviewedBy = draft.shipItReviewers;
            draft.shipItReviewers = null;
            return;
        }

        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(mergeRequest.projectId, mergeRequest.iid);
        if (approvals.approvedBy == null || approvals.approvedBy.length == 0) {
            log.info("No approvals found for merge request");
            return;
        }

        draft.reviewedBy = Arrays.stream(mergeRequest.appovedBy)
                .map(approvalUser -> approvalUser.user.username).collect(Collectors.joining(","));
    }
}
