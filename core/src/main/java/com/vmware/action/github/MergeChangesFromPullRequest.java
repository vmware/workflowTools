package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;

@ActionDescription("Performs a git pull against a pull request using merge")
public class MergeChangesFromPullRequest extends BaseCommitWithPullRequestAction {
    public MergeChangesFromPullRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        log.info("Merging latest changes from {}", pullRequest.head.ref);
        git.fetch();
        git.merge(gitRepoConfig.defaultGitRemote + "/" + pullRequest.head.ref);
    }
}
