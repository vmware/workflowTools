package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;

@ActionDescription("Marks pull request as ready for review")
public class MarkPullRequestAsReadyForReview extends BaseCommitWithPullRequestAction {
    public MarkPullRequestAsReadyForReview(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        skipActionIfTrue(!pullRequest.draft, "pull request is not a draft");
        github.markPullRequestAsReadyForReview(pullRequest);
    }
}
