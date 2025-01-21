package com.vmware.action.github;

import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.GraphqlResponse;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.Review;
import com.vmware.github.domain.ReviewNode;
import com.vmware.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Checks the status of review approvals for a pull request")
public class CheckStatusOfPullRequestApprovals extends BaseCommitWithPullRequestAction {
    public CheckStatusOfPullRequestApprovals(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        PullRequest pullRequest = draft.getGithubPullRequest();
        GraphqlResponse.PullRequestNode pullRequestNode = github.getPullRequestViaGraphql(pullRequest);
        log.info("Pull request approval status: {}", pullRequestNode.reviewDecision);
        List<String> approvers = pullRequestNode.approvers();
        if (approvers.isEmpty()) {
            log.info("Not approved by any reviewers yet");
        } else {
            log.info("Pull request {} approved by {}", pullRequest.number, StringUtils.join(approvers));
        }
    }
}
