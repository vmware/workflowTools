package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.Review;
import com.vmware.github.domain.ReviewNode;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Exists if the pull request does not have all required approvals")
public class ExitIfPullRequestDoesNotHaveRequiredApprovals extends BaseCommitWithPullRequestAction {
    public ExitIfPullRequestDoesNotHaveRequiredApprovals(WorkflowConfig config) {
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
        PullRequest pullRequest = draft.getGithubPullRequest();
        GraphqlResponse.PullRequestNode pullRequestNode = github.getPullRequestViaGraphql(pullRequest);
        List<String> approvers = pullRequestNode.approvers();
        if (approvers.isEmpty()) {
            cancelWithMessage("no approved reviews found for pull request {}", pullRequest.htmlUrl);
        } else {
            draft.shipItReviewers = StringUtils.join(approvers);
            if (pullRequestNode.reviewDecision == GraphqlResponse.PullRequestReviewDecision.REVIEW_REQUIRED) {
                cancelWithMessage("still need more approvals, already approved by {}", draft.shipItReviewers);
            }
            log.info("Approved by {}", draft.shipItReviewers);
        }
    }
}
