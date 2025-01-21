package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequestForUpdate;

@ActionDescription("Close the pull request matching the commit.")
public class ClosePullRequest extends BaseCommitWithPullRequestAction {

    public ClosePullRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        log.info("Closing pull request {}", draft.requestUrl);
        github.closePullRequest(draft.getGithubPullRequest());
    }
}
