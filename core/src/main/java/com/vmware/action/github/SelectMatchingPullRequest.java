package com.vmware.action.github;

import com.vmware.action.base.BaseCommitUsingGithubAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;

import java.util.Optional;

@ActionDescription("Selects the matching pull request in Github by merge branch.")
public class SelectMatchingPullRequest extends BaseCommitUsingGithubAction {
    public SelectMatchingPullRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String sourceMergeBranch = determineSourceMergeBranch();
        String targetMergeBranch = determineTargetMergeBranch();
        log.info("Checking pull requests for request matching source branch {} and target branch {}", sourceMergeBranch, targetMergeBranch);

        Optional<PullRequest> matchingRequest = github.getPullRequestForSourceAndTargetBranch(githubConfig.githubRepoOwnerName,
                githubConfig.githubRepoName, sourceMergeBranch, targetMergeBranch);
        if (matchingRequest.isPresent()) {
            log.info("Found matching pll request {}", matchingRequest.get().htmlUrl);
            draft.setGithubPullRequest(matchingRequest.get());
        } else {
            if (gitRepoConfig.failIfNoRequestFound) {
                cancelWithMessage("no matching pull request was found");
            } else {
                log.info("Failed to find matching pull request");
            }
        }
    }
}
