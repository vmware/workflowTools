package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.Review;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.util.exception.FatalException;

import java.util.Collections;
import java.util.List;

import static com.vmware.action.base.BaseSetUsersList.CandidateSearchType.gitlab;

@ActionDescription("Approve pull request in Github.")
public class ApprovePullRequest extends BaseCommitWithPullRequestAction {
    public ApprovePullRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        List<Review> approvals = github.getApprovedReviewsForPullRequest(pullRequest);
        if (approvals.stream().anyMatch(approval -> approval.user.login.equals(github.getUsername()))) {
            log.info("Pull request {} has already been self approved", draft.requestUrl);
            return;
        }

        log.info("Self approving pull request {}", draft.requestUrl);
        Review review = github.approvePullRequest(pullRequest);
        if (!review.isApproved()) {
            throw new FatalException("Pull request {} is not self approved", draft.requestUrl);
        }
    }
}
