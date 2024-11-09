package com.vmware.action.base;

import com.vmware.AutocompleteUser;
import com.vmware.config.WorkflowConfig;
import com.vmware.github.Github;
import com.vmware.github.domain.PullRequest;
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

public abstract class BaseCommitUsingGithubAction extends BaseCommitAction {
    protected Github github;

    public BaseCommitUsingGithubAction(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("githubUrl", "githubRepoOwnerName", "githubRepoName");
    }

    @Override
    public void asyncSetup() {
        super.asyncSetup();
        github = serviceLocator.getGithub();
    }

    protected Set<AutocompleteUser> determineReviewersToAdd(PullRequest pullRequest) {
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

    protected Set<AutocompleteUser> determineReviewersToRemove(PullRequest pullRequest) {
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
