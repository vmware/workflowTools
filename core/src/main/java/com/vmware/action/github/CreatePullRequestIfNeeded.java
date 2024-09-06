package com.vmware.action.github;

import com.vmware.action.base.BaseCommitUsingGithubAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequestForUpdate;
import com.vmware.github.domain.PullRequest;

@ActionDescription("Creates a pull request in github, uses pull request branch format unless one specified.")
public class CreatePullRequestIfNeeded extends BaseCommitUsingGithubAction {
    public CreatePullRequestIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.hasMergeOrPullRequest(), "pull request with url " + draft.requestUrl + " has already been created");
    }

    @Override
    public void process() {
        PullRequestForUpdate pullRequest = new PullRequestForUpdate();
        pullRequest.repoOwner = githubConfig.githubRepoOwnerName;
        pullRequest.title = draft.summary;
        pullRequest.body = draft.description;
        pullRequest.repoName = githubConfig.githubRepoName;
        pullRequest.head = determineSourceMergeBranch();
        pullRequest.base = determineTargetMergeBranch();
        pullRequest.draft = gitRepoConfig.markAsDraft;

        log.info("Creating pull request with source branch {} and target branch {}", pullRequest.head, pullRequest.base);
        PullRequest createdRequest = github.createPullRequest(pullRequest);
        draft.setGithubPullRequest(createdRequest);
        log.info("Created pull request {}", createdRequest.htmlUrl);
    }
}
