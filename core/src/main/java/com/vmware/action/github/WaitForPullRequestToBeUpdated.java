package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.util.ThreadUtils;

import java.util.concurrent.Callable;

@ActionDescription("Wait for github merge request commit hash to match the commit hash of the current branch head.")
public class WaitForPullRequestToBeUpdated extends BaseCommitWithPullRequestAction {

    public WaitForPullRequestToBeUpdated(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        String headRef = git.revParse("HEAD");
        String currentBranch = git.currentBranch();
        if (headRef.equals(draft.getGitlabMergeRequest().sha)) {
            log.debug("Pull request {} commit hash already matches branch {} ref {}", draft.mergeRequestId(), currentBranch, headRef);
            return;
        }
        log.info("Waiting for pull request {} commit hash to be updated to match branch {} ref {}", draft.mergeRequestId(), currentBranch, headRef);
        Callable<Boolean> commitHashCheck = () -> {
            PullRequest pullRequest = github.getPullRequest(githubConfig.githubRepoOwnerName, githubConfig.githubRepoName, draft.mergeRequestId());
            draft.setGithubPullRequest(pullRequest);
            log.info("Current pull request commit hash " + pullRequest.head.sha);
            return headRef.equals(pullRequest.head.sha);
        };

        ThreadUtils.waitForCallable(commitHashCheck, config.waitTimeForBlockingWorkflowAction, 3,
                "Pull request failed to be updated with sha " + headRef);
    }
}
