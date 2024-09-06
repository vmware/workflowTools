package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.Review;
import com.vmware.util.StringUtils;

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
        List<Review> approvedReviews = github.getApprovedReviewsForPullRequest(pullRequest);
        if (approvedReviews.isEmpty()) {
            cancelWithMessage("no approved reviews found for pull request {}", pullRequest.htmlUrl);
        } else {
            draft.shipItReviewers = approvedReviews.stream().map(review -> review.user.login).collect(Collectors.joining(","));
            log.info("Set reviewers to {}", draft.shipItReviewers);
        }
    }
}
