package com.vmware.action.base;

import com.vmware.AutocompleteUser;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.Gitlab;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Link;
import com.vmware.util.StringUtils;
import com.vmware.util.collection.OverwritableSet;
import com.vmware.util.input.CommaArgumentDelimeter;
import com.vmware.util.input.ImprovedStringsCompleter;
import com.vmware.util.input.InputUtils;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.vmware.util.StringUtils.isInteger;

public abstract class BaseSetUsersList extends BaseCommitReadAction {
    protected final CandidateSearchType searchType;
    private final boolean addToReviewerList;

    private Link usersLink;

    public BaseSetUsersList(WorkflowConfig config, String propertyName, CandidateSearchType searchType, boolean addToUserList) {
        super(config, propertyName);
        this.searchType = searchType;
        this.addToReviewerList = addToUserList;
    }

    @Override
    public void asyncSetup() {
    }

    @Override
    public void preprocess() {
        if (searchType == CandidateSearchType.reviewboard) {
            serviceLocator.getReviewBoard().setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
        } else if (searchType == CandidateSearchType.gitlab) {
            serviceLocator.getGitlab().setupAuthenticatedConnection();
        } else if (searchType == CandidateSearchType.github) {
            serviceLocator.getGithub().setupAuthenticatedConnection();
        }
    }

    protected String readUsers(Set<String> additionalOptions, String existingUserTest, String userDescription) {
        Completer userCompleter = createUserCompleter(additionalOptions);
        ArgumentCompleter argumentCompleter = new ArgumentCompleter(new CommaArgumentDelimeter(), userCompleter);
        argumentCompleter.setStrict(false);

        if (searchType != CandidateSearchType.none) {
            log.info("Tab can be used to autocomplete names");
            log.info("After entering 3 characters, {} is searched for a user", searchType.name());
        }
        String users = InputUtils.readValue(userDescription, argumentCompleter);
        String existingUsers = addToReviewerList && StringUtils.isNotEmpty(draft.reviewedBy) ? existingUserTest + "," : "";
        return generateUserList(existingUsers + users);
    }

    private Completer createUserCompleter(Set<String> autocompleteOptions) {
        if (searchType != CandidateSearchType.none) {
            Function<String, List<? extends AutocompleteUser>> searchFunction = createSearchFunction();
            return new UserCompleter(searchFunction, autocompleteOptions);
        } else {
            ImprovedStringsCompleter completer = new ImprovedStringsCompleter(autocompleteOptions);
            completer.setDelimeterText("");
            return completer;
        }
    }

    private Function<String, List<? extends AutocompleteUser>> createSearchFunction() {
        if (searchType == CandidateSearchType.reviewboard) {
            return (buffer) -> {
                if (usersLink == null) {
                    usersLink = serviceLocator.getReviewBoard().getRootLinkList().getUsersLink();
                }
                return serviceLocator.getReviewBoard().searchUsersMatchingText(usersLink, buffer,
                        reviewBoardConfig.searchByUsernamesOnly);
            };
        } else if (searchType == CandidateSearchType.gitlab) {
            return (buffer) -> serviceLocator.getGitlab().searchUsers(buffer);
        } else if (searchType == CandidateSearchType.github) {
            return (buffer) -> serviceLocator.getGithub().searchUsers(githubConfig.githubRepoOwnerName, buffer);
        } else {
            return null;
        }
    }

    private String generateUserList(String usersText) {
        if (usersText.trim().isEmpty()) {
            return commitConfig.trivialReviewerLabel;
        }

        Collection<String> parsedUsers = new OverwritableSet.UniqueArrayList<>();
        String[] users = usersText.split(",");
        for (String user : users) {
            String fragment = user.trim();
            if (fragment.isEmpty()) {
                continue;
            }
            Collection<String> reviewersInGroup = getReviewersInGroup(fragment);
            if (reviewersInGroup != null) {
                addReviewers(parsedUsers, reviewersInGroup);
                continue;
            }

            fragment = convertFragmentToUsername(fragment);
            addReviewer(parsedUsers, fragment);
        }
        return StringUtils.join(parsedUsers);
    }

    private String convertFragmentToUsername(String fragment) {
        if (!fragment.contains("(")) {
            return fragment;
        }
        return fragment.substring(0, fragment.indexOf("(")).trim();
    }

    private Set<String> getReviewersInGroup(String fragment) {
        LinkedHashMap<String, SortedSet<String>> reviewerGroups = commitConfig.reviewerGroups;
        if (reviewerGroups == null) {
            return null;
        }

        int possibleIndexNumber = isInteger(fragment) ? Integer.parseInt(fragment) : -1;
        int count = 1;
        for (String groupName : reviewerGroups.keySet()) {
            if (groupName.equalsIgnoreCase(fragment) || possibleIndexNumber == count++) {
                return reviewerGroups.get(groupName);
            }
        }
        return null;
    }

    private void addReviewers(Collection<String> parsedReviewers, Collection<String> reviewersToAdd) {
        for (String reviewerToAdd : reviewersToAdd) {
            addReviewer(parsedReviewers, reviewerToAdd);
        }
    }

    private void addReviewer(Collection<String> parsedReviewers, String fragment) {
        if (fragment.equals(config.username)) {
            log.debug("Not adding myself ({}) to reviewer list", config.username);
            return;
        }
        parsedReviewers.add(fragment);
    }

    private class UserCompleter extends ImprovedStringsCompleter {

        private final Set<String> searchedValues = new HashSet<>();

        private final Function<String, List<? extends AutocompleteUser>> userSearchFunction;

        UserCompleter(Function<String, List<? extends AutocompleteUser>> userSearchFunction, Collection<String> valuesShownWhenNoBuffer) {
            this.userSearchFunction = userSearchFunction;
            this.valuesShownWhenNoBuffer.addAll(valuesShownWhenNoBuffer);
            super.setDelimeterText("");
            super.setCaseInsensitiveMatching(true);
        }

        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            if (StringUtils.isEmpty(buffer)) {
                return super.complete(buffer, cursor, candidates);
            }
            values.addAll(valuesShownWhenNoBuffer);
            if (buffer.length() < 3) {
                return super.complete(buffer, cursor, candidates);
            }
            if (!searchedValues.contains(buffer)) {
                searchedValues.add(buffer);
                if (userSearchFunction != null) {
                    List<? extends AutocompleteUser> users = userSearchFunction.apply(buffer);
                    for (AutocompleteUser user : users) {
                        values.add(String.format("%s (%s)", user.username(), user.fullName()));
                    }
                }
            }

            return super.complete(buffer, cursor, candidates);
        }

        @Override
        protected void addMatchingValueToCandidates(List<CharSequence> candidates, String value) {
            boolean addValue = true;
            for (int i = candidates.size() -1; i >= 0; i--) {
                String candidateAsString = candidates.get(i).toString();
                // prevent dupes of local usernames with remote users
                if (candidateAsString.startsWith(value + " (")) {
                    addValue = false;
                    break;
                } else if (value.startsWith(candidateAsString + " (")) {
                    candidates.remove(i);
                    candidates.add(i, value);
                    addValue = false;
                    break;
                }
            }
            if (addValue) {
                candidates.add(value);
            }
        }

        @Override
        protected boolean bufferMatchesValue(String buffer, String value) {
            if (!value.contains("(")) {
                return super.bufferMatchesValue(buffer, value);
            }
            CandidateUser candidateUser = CandidateUser.fromText(value);
            if (candidateUser == null) {
                log.debug("Null user for value " + value);
                return super.bufferMatchesValue(buffer, value);
            }
            return super.bufferMatchesValue(buffer, value) || super.bufferMatchesValue(buffer, candidateUser.username)
                    || super.bufferMatchesValue(buffer, candidateUser.fullName())
                    || super.bufferMatchesValue(buffer, candidateUser.firstName) || super.bufferMatchesValue(buffer, candidateUser.lastName);
        }
    }

    public enum CandidateSearchType {
        none,
        reviewboard,
        gitlab,
        github
    }

    private static class CandidateUser implements AutocompleteUser {
        private static final Logger log = LoggerFactory.getLogger(CandidateUser.class);
        private static final Pattern userPattern = Pattern.compile("(\\w+)\\s+\\((.+?)\\s+(.+?)\\)");

        private final String username;
        private final String firstName;
        private final String lastName;

        public CandidateUser(String username, String firstName, String lastName) {
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public String username() {
            return username;
        }

        @Override
        public String fullName() {
            return firstName + " " + lastName;
        }

        public static CandidateUser fromText(String sourceText) {
            Matcher userMatcher = userPattern.matcher(sourceText);
            if (!userMatcher.find()) {
                log.debug("Failed to match user value {} with pattern {}", sourceText, userPattern.pattern());
                return null;
            }
            return new CandidateUser(userMatcher.group(1), userMatcher.group(2), userMatcher.group(3));
        }
    }
}
