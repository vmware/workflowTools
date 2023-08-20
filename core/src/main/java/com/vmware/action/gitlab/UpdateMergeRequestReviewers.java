package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.gitlab.domain.User;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ActionDescription("Updates the reviewers for the merge request")
public class UpdateMergeRequestReviewers extends BaseCommitWithMergeRequestAction {
    public UpdateMergeRequestReviewers(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(StringUtils.isLong(draft.id), "as reviewboard request " + draft.id + " is associated with this commit");
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        MergeRequestApprovals approvals =
                gitlab.getMergeRequestApprovals(mergeRequest.projectId, mergeRequest.iid);
        final Set<User> approversToCheck = new LinkedHashSet<>(Arrays.asList(approvals.suggestedApprovers));
        Arrays.stream(approvals.approvedBy).forEach(approvalUser -> approversToCheck.add(approvalUser.user));

        List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
        usernames.removeIf(StringUtils::isInteger);
        Set<User> users = usernames.stream().map(username -> {
            Optional<User> matchingUser = approversToCheck.stream()
                    .filter(approver -> username.equals(approver.username)).findFirst();
            if (!matchingUser.isPresent()) {
                log.info("No user found for username {}", username);
            }
            return matchingUser;
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        updateMergeRequestReviewersToMatch(users);
    }

    private void updateMergeRequestReviewersToMatch(Set<User> users) {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        long[] reviewerIds = users.stream().mapToLong(user -> user.id).toArray();
        if (reviewerIds == mergeRequest.reviewerIds) {
            log.info("Reviewer already match for merge request");
            return;
        }

        log.debug("Updating reviewers for merge request");
        mergeRequest.reviewerIds = reviewerIds;
        draft.setGitlabMergeRequest(gitlab.updateMergeRequest(mergeRequest));

    }
}
