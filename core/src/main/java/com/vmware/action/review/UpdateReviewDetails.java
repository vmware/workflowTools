package com.vmware.action.review;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.Link;
import com.vmware.reviewboard.domain.RepoType;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;

@ActionDescription("Updates the review request draft details (summary, description, testing done, bug number, groups, people).")
public class UpdateReviewDetails extends BaseCommitUsingReviewBoardAction {
    public UpdateReviewDetails(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        ReviewRequest reviewRequest = draft.reviewRequest;
        log.info("Updating information for review " + reviewRequest.id);

        ReviewRequestDraft existingDraft = reviewBoard.getReviewRequestDraftWithExceptionHandling(reviewRequest.getDraftLink());
        if (existingDraft != null) {
            draft.targetGroups = existingDraft.targetGroups;
            if (StringUtils.isEmpty(draft.reviewedBy)) {
                draft.reviewedBy = existingDraft.reviewedBy;
                log.info("Keeping reviewers {} from draft", draft.reviewedBy);
            }
        } else if (StringUtils.isEmpty(draft.reviewedBy)) {
            draft.reviewedBy = reviewRequest.getTargetReviewersAsString();
            log.info("Keeping reviewers {} from review request", draft.reviewedBy);
        }

        if (reviewBoardConfig.disableMarkdown) {
            log.info("Sending description and testing done as plain text");
            draft.descriptionTextType = "plain";
            draft.testingDoneTextType = "plain";
        } else {
            log.info("Sending description and testing done as markdown text");
            draft.descriptionTextType = "markdown";
            draft.testingDoneTextType = "markdown";
        }
        draft.commitId = determineCommitId();
        log.debug("Review commit id set to {}", draft.commitId);

        draft.updateTargetGroupsIfNeeded(reviewBoardConfig.targetGroups);
        draft.addExtraTargetGroupsIfNeeded();
        draft.dependsOnRequests = determineDependsOnRequestIds();
        if (commitConfig.noReviewerLabel.equals(draft.reviewedBy)) {
            log.debug("Setting reviewedBy to null as it matches the no reviewer label {}", commitConfig.noReviewerLabel);
            draft.reviewedBy = null;
        }
        reviewBoard.updateReviewRequestDraft(reviewRequest.getDraftLink(), draft);
        if (draft.reviewedBy == null) {
            draft.reviewedBy = commitConfig.noReviewerLabel;
        }
        log.info("Successfully updated review information");
    }

    private String determineDependsOnRequestIds() {
        if (!git.workingDirectoryIsInGitRepo()) {
            return null;
        }
        String mergeBase = git.mergeBase(gitRepoConfig.trackingBranchPath(), "HEAD");
        String mergeBaseRef = git.revParse(mergeBase);
        int counter = 1;
        String lastCommitRef = git.revParse("HEAD~" + counter);
        List<String> linksForDependantRequests = new ArrayList<>();
        while (!lastCommitRef.equals(mergeBaseRef)) {
            String commitText = git.commitText(counter++);
            ReviewRequestDraft draftForCommit = new ReviewRequestDraft(commitText, commitConfig);
            if (draftForCommit.hasReviewNumber()) {
                linksForDependantRequests.add(draftForCommit.id);
            }
            lastCommitRef = git.revParse("HEAD~" + counter);
        }
        return String.join(",", linksForDependantRequests);
    }

    private String determineCommitId() {
        RepoType repoType = draft.repoType;
        if (repoType == RepoType.perforce && !git.workingDirectoryIsInGitRepo()) {
            return draft.perforceChangelistId;
        } else if (git.workingDirectoryIsInGitRepo()) {
            return git.revParse("HEAD");
        } else {
            return null;
        }
    }
}