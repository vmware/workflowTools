package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.PullRequestUpdateBranchResponse;
import com.vmware.util.exception.FatalException;

@ActionDescription("Updates pull request branch against the top of the master / main branch")
public class UpdatePullRequestBranch extends BaseCommitWithPullRequestAction {
    public UpdatePullRequestBranch(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        log.info("Updating branch for pull request {}", pullRequest.number);
        PullRequestUpdateBranchResponse response = github.updatePullRequestBranch(pullRequest);
        if (response.message == null || !response.message.contains("Updating")) {
            throw new FatalException(response.message);
        }
    }
}
