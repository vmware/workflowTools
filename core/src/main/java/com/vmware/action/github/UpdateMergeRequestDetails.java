package com.vmware.action.github;

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

import static com.vmware.util.StringUtils.replaceLineBreakWithHtmlBrTag;

@ActionDescription("Updates the title, description and reviewers for a merge request")
public class UpdateMergeRequestDetails extends BaseCommitWithMergeRequestAction {
    public UpdateMergeRequestDetails(WorkflowConfig config) {
        super(config, true, true);
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        log.info("Updating details for merge request {}", mergeRequest.iid);
        mergeRequest.title = gitlabConfig.markAsDraft ? gitlabConfig.draftMergeRequestPrefix + " " + draft.summary : draft.summary;
        mergeRequest.description = replaceLineBreakWithHtmlBrTag(draft.description) + "\n\n" + commitConfig.testingDoneLabel
                + " " + replaceLineBreakWithHtmlBrTag(draft.testingDone);
        if (draft.hasReviewNumber()) {
            log.debug("Not setting reviewer ids as merge request is already associated with a reviewboard review");
        } else {
            mergeRequest.reviewerIds = determineReviewerIds(mergeRequest);
        }
        draft.setGitlabMergeRequest(gitlab.updateMergeRequest(mergeRequest));
    }

    private long[] determineReviewerIds(MergeRequest mergeRequest) {
        if (draft.isTrivialCommit(commitConfig.trivialReviewerLabel)) {
            return new long[0];
        }
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
        return users.stream().mapToLong(user -> user.id).toArray();
    }

}
