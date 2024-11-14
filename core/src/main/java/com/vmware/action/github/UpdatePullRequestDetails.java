package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.PullRequestForUpdate;

@ActionDescription("Updates the title, description and reviewers for a pull request")
public class UpdatePullRequestDetails extends BaseCommitWithPullRequestAction {
    public UpdatePullRequestDetails(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        PullRequestForUpdate pullRequestForUpdate = pullRequest.pullRequestForUpdate();
        String targetBranch = determineTargetMergeBranch();
        if (!targetBranch.equals(pullRequest.base.ref)) {
            pullRequestForUpdate.head = targetBranch;
        }
        log.info("Updating details for pull request {}", pullRequest.htmlUrl);
        pullRequestForUpdate.title = draft.summary;
        pullRequestForUpdate.body = draft.toText(commitConfig, false, false);
        if (draft.hasReviewNumber()) {
            log.debug("Not setting reviewer ids as pull request is already associated with a reviewboard review");
        } else if (draft.hasReviewers()) {
            github.addReviewersToPullRequest(pullRequest, determineReviewersToAdd(pullRequest));
            github.removeReviewersFromPullRequest(pullRequest, determineReviewersToRemove(pullRequest));
        }
        PullRequest updatedPullRequest = github.updatePullRequest(pullRequestForUpdate);
        draft.setGithubPullRequest(updatedPullRequest);
    }
}
