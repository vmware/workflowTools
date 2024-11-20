package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.util.exception.FatalException;

@ActionDescription("Merge pull request in Github so that it is merged to the target branch.")
public class MergePullRequest extends BaseCommitWithPullRequestAction {
    public MergePullRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        log.info("Merging pull request {}", draft.requestUrl);
        PullRequest pullRequest = draft.getGithubPullRequest();

        String upstreamRef = gitRepoConfig.defaultGitRemote + "/" + pullRequest.base.ref;
        int commitCount = git.getCommitCountSinceRef(upstreamRef);
        if (commitCount != 1) {
            throw new FatalException("Pull request {} has {} commits since {}. Please merge via UI. Can only merge via workflow tools if there is one commit",
                    pullRequest.number, commitCount, upstreamRef);
        }
        String headRef = git.revParse("head");
        if (!headRef.equals(pullRequest.head.sha)) {
            throw new FatalException("Cannot merge pull request {} as current head ref {} does not match pull request head ref {}",
                    pullRequest.number, headRef, pullRequest.head.sha);
        }
        github.mergePullRequest(pullRequest, githubConfig.mergeMethod, draft.summary, draft.toText(commitConfig, false, false));
    }
}
