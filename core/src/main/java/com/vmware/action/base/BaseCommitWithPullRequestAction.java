package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithPullRequestAction extends BaseCommitUsingGithubAction {
    private final boolean loadPullRequest;
    private final boolean skipIfNoPullRequest;

    public BaseCommitWithPullRequestAction(WorkflowConfig config) {
        this(config, false, false);
    }

    public BaseCommitWithPullRequestAction(WorkflowConfig config, boolean loadPullRequest) {
        this(config, loadPullRequest, false);
    }


    public BaseCommitWithPullRequestAction(WorkflowConfig config, boolean loadPullRequest, boolean skipIfNoPullRequest) {
        super(config);
        this.loadPullRequest = loadPullRequest;
        this.skipIfNoPullRequest = skipIfNoPullRequest;
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (!skipIfNoPullRequest) {
            super.failIfTrue(!draft.hasMergeOrPullRequest(), "no github pull request associated with commit");
        }
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (skipIfNoPullRequest) {
            super.skipActionIfTrue(!draft.hasMergeOrPullRequest(), "no github pull request associated with commit");
        }
    }

    @Override
    public void preprocess() {
        super.preprocess();
        if (loadPullRequest && draft.getGithubPullRequest() == null) {
            draft.setGithubPullRequest(github.getPullRequest(githubConfig.githubRepoOwnerName, githubConfig.githubRepoName, draft.mergeRequestId()));
        }
    }
}
