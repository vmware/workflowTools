package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.Review;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovals;

import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Checks the status of review approvals for a pull request")
public class CheckStatusOfPullRequestApprovals extends BaseCommitWithPullRequestAction {
    public CheckStatusOfPullRequestApprovals(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        List<Review> approvedReviews = github.getApprovedReviewsForPullRequest(pullRequest);
        if (approvedReviews.isEmpty()) {
            log.info("Not approved by any reviewers yet");
        } else {
            String approvedReviewers = approvedReviews.stream().map(review -> review.user.login).collect(Collectors.joining(","));
            log.info("Pull request {} approved by {}", pullRequest.number, approvedReviewers);
        }
    }
}
