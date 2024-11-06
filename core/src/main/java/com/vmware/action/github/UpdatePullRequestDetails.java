package com.vmware.action.github;

import com.vmware.AutocompleteUser;
import com.vmware.action.base.BaseCommitWithPullRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.domain.PullRequest;
import com.vmware.github.domain.PullRequestForUpdate;
import com.vmware.github.domain.Team;
import com.vmware.github.domain.User;
import com.vmware.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        pullRequestForUpdate.body = draft.description + "\n" + commitConfig.getTestingDoneLabel() + draft.testingDone;
        if (draft.hasReviewNumber()) {
            log.debug("Not setting reviewer ids as pull request is already associated with a reviewboard review");
        } else {
            github.addReviewersToPullRequest(pullRequest, determineReviewersToAdd(pullRequest));
            github.removeReviewersFromPullRequest(pullRequest, determineReviewersToRemove(pullRequest));
        }
        PullRequest updatedPullRequest = github.updatePullRequest(pullRequestForUpdate);
        draft.setGithubPullRequest(updatedPullRequest);
    }

    private Set<AutocompleteUser> determineReviewersToAdd(PullRequest pullRequest) {
        if (draft.isTrivialCommit(commitConfig.trivialReviewerLabel)) {
            return Collections.emptySet();
        }
        final Set<User> reviewersToCheck = new LinkedHashSet<>(Arrays.asList(pullRequest.requestedReviewers));
        final Set<Team> teamsToCheck = new LinkedHashSet<>(Arrays.asList(pullRequest.requestedTeams));
        List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
        usernames.removeIf(StringUtils::isInteger);
        return usernames.stream().map(username -> {
            Optional<User> matchingUser = reviewersToCheck.stream()
                    .filter(approver -> username.equals(approver.login)).findFirst();
            Optional<Team> matchingTeam = teamsToCheck.stream()
                    .filter(team -> username.equals(team.username())).findFirst();
            if (!matchingUser.isPresent() && !matchingTeam.isPresent()) {
                if (username.startsWith("@")) {
                    return new Team(username.substring(1));
                } else {
                    return new User(username, null);
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<AutocompleteUser> determineReviewersToRemove(PullRequest pullRequest) {
        if (draft.isTrivialCommit(commitConfig.trivialReviewerLabel)) {
            return Collections.emptySet();
        }
        final Set<User> reviewersToCheck = new LinkedHashSet<>(Arrays.asList(pullRequest.requestedReviewers));
        final Set<Team> teamsToCheck = new LinkedHashSet<>(Arrays.asList(pullRequest.requestedTeams));
        List<String> usernames = StringUtils.splitAndTrim(draft.reviewedBy, ",");
        usernames.removeIf(StringUtils::isInteger);
        return Stream.of(reviewersToCheck, teamsToCheck).flatMap(Collection::stream).filter(reviewer -> {
            Optional<String> matchingUserOrTeam = usernames.stream()
                    .filter(username -> username.equals(reviewer.username())).findFirst();
            return !matchingUserOrTeam.isPresent();
        }).collect(Collectors.toSet());
    }

}
