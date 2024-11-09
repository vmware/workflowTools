package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;

@ActionDescription("Marks pull request as a draft")
public class ConvertPullRequestToDraft extends BaseCommitWithPullRequestAction {
    public ConvertPullRequestToDraft(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        skipActionIfTrue(pullRequest.draft, "pull request is a draft");
        github.updatePullRequestDraftState(pullRequest, true);
    }
}
