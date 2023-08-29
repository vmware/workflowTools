package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.Group;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovalRule;
import com.vmware.gitlab.domain.User;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ActionDescription("Updates the approval rule for the merge request")
public class UpdateMergeRequestApprovers extends BaseCommitWithMergeRequestAction {
    public UpdateMergeRequestApprovers(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(StringUtils.isLong(draft.id), "as review request " + draft.id + " is associated with this commit");
    }

    @Override
    public void process() {
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        MergeRequestApprovalRule[] approvalRules =
                gitlab.getMergeRequestApprovalRules(mergeRequest.projectId, mergeRequest.iid);
        Optional<MergeRequestApprovalRule> matchingRule = Arrays.stream(approvalRules)
                .filter(rule -> gitlabConfig.approvalRuleName.equals(rule.name)).findFirst();

        Set<String> reviewersFromDraft = StringUtils.isEmpty(draft.reviewedBy) ? Collections.emptySet() :
                Arrays.stream(draft.reviewedBy.split(",")).filter(value -> !StringUtils.isLong(value))
                        .map(String::trim).collect(Collectors.toSet());
        if (gitlabConfig.allowSelfApproval) {
            log.debug("Adding {} as an approver", gitlab.getUsername());
            reviewersFromDraft.add(gitlab.getUsername());
        }
        Set<String> groupsFromDraft = StringUtils.isEmpty(draft.reviewedBy) ? Collections.emptySet() :
                Arrays.stream(draft.reviewedBy.split(",")).filter(StringUtils::isLong)
                        .map(String::trim).collect(Collectors.toSet());
        if (matchingRule.isPresent()) {
            MergeRequestApprovalRule rule = matchingRule.get();
            boolean reviewersAreTheSame = usersAreTheSame(rule, reviewersFromDraft, groupsFromDraft);
            if (reviewersAreTheSame) {
                log.info("No need to update approval rule {} as reviewers and groups are the same", rule.name);
            } else if (hasNoRealReviewers(reviewersFromDraft, groupsFromDraft)) {
                log.info("Deleting approval rule {}", rule.name);
                gitlab.deleteMergeRequestApprovalRule(mergeRequest.projectId, mergeRequest.iid, rule.id);
            } else {
                log.info("Updating approval rule {}", rule.name);
                setInfoFromDraft(rule, reviewersFromDraft, groupsFromDraft);
                gitlab.updateMergeRequestApprovalRule(mergeRequest.projectId, mergeRequest.iid, rule);
            }
        } else {
            if (hasNoRealReviewers(reviewersFromDraft, groupsFromDraft)) {
                log.info("No need to create approval rule as no reviewers or groups are specified");
                return;
            }
            log.info("Creating new approval rule with name {}", gitlabConfig.approvalRuleName);
            MergeRequestApprovalRule rule = new MergeRequestApprovalRule();
            rule.name = gitlabConfig.approvalRuleName;
            setInfoFromDraft(rule, reviewersFromDraft, groupsFromDraft);
            gitlab.createMergeRequestApprovalRule(mergeRequest.projectId, mergeRequest.iid, rule);
        }
    }

    private void setInfoFromDraft(MergeRequestApprovalRule rule, Set<String> reviewersFromDraft, Set<String> groupsFromDraft) {
        rule.approvalsRequired = gitlabConfig.approvalsRequired;
        rule.usernames = reviewersFromDraft.toArray(new String[0]);
        rule.groupIds = groupsFromDraft.stream().mapToLong(Long::parseLong).toArray();
    }

    private boolean hasNoRealReviewers(Set<String> reviewersFromDraft, Set<String> groupsFromDraft) {
        if (!groupsFromDraft.isEmpty()) {
            return false;
        }
        return reviewersFromDraft.isEmpty() ||
                (reviewersFromDraft.size() == 1&& reviewersFromDraft.contains(commitConfig.trivialReviewerLabel));
    }

    private boolean usersAreTheSame(MergeRequestApprovalRule rule, Set<String> reviewersFromDraft, Set<String> groupsFromDraft) {
        User[] reviewersForRule = rule.users != null ? rule.users : new User[0];
        Group[] groupsForRule = rule.groups != null ? rule.groups : new Group[0];

        if (reviewersForRule.length != reviewersFromDraft.size() || groupsForRule.length != groupsFromDraft.size()) {
            return false;
        }

        return Arrays.stream(reviewersForRule).allMatch(user -> reviewersFromDraft.contains(user.username)) &&
                Arrays.stream(groupsForRule).allMatch(group -> groupsFromDraft.contains(String.valueOf(group.id)));
    }
}
