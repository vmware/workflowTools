package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Link;
import com.vmware.reviewboard.domain.ReviewUser;
import com.vmware.util.StringUtils;
import com.vmware.util.collection.OverwritableSet;
import com.vmware.util.input.CommaArgumentDelimeter;
import com.vmware.util.input.ImprovedStringsCompleter;
import com.vmware.util.input.InputUtils;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.vmware.util.StringUtils.isInteger;

public abstract class BaseSetUsersList extends BaseCommitReadAction {
    private boolean searchReviewBoardForUsers;
    private boolean addToReviewerList;
    private ReviewBoard reviewboard;

    public BaseSetUsersList(WorkflowConfig config, String propertyName, boolean searchReviewBoardForUsers, boolean addToUserList) {
        super(config, propertyName);
        this.searchReviewBoardForUsers = searchReviewBoardForUsers;
        this.addToReviewerList = addToUserList;
    }

    @Override
    public void asyncSetup() {
        if (searchReviewBoardForUsers) {
            this.reviewboard = serviceLocator.getReviewBoard();
        }
    }

    @Override
    public void preprocess() {
        if (searchReviewBoardForUsers) {
            this.reviewboard.setupAuthenticatedConnectionWithLocalTimezone(reviewBoardConfig.reviewBoardDateFormat);
        }
    }

    protected String readUsers(Set<String> additionalOptions, String existingUserTest, String userDescription) {
        Completer userCompleter = createUserCompleter(additionalOptions);
        ArgumentCompleter argumentCompleter = new ArgumentCompleter(new CommaArgumentDelimeter(), userCompleter);
        argumentCompleter.setStrict(false);

        if (searchReviewBoardForUsers) {
            log.info("Tab can be used to autocomplete names");
            log.info("After entering 3 characters, reviewboard is searched for a user");
        }
        String users = InputUtils.readValue(userDescription, argumentCompleter);
        String existingUsers = addToReviewerList && StringUtils.isNotEmpty(draft.reviewedBy) ? existingUserTest + "," : "";
        return generateUserList(existingUsers + users);
    }

    private Completer createUserCompleter(Set<String> autocompleteOptions) {
        if (searchReviewBoardForUsers) {
            return new ReviewboardUserCompleter(serviceLocator.getReviewBoard(), autocompleteOptions);
        } else {
            ImprovedStringsCompleter completer = new ImprovedStringsCompleter(autocompleteOptions);
            completer.setDelimeterText("");
            return completer;
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
        LinkedHashMap<String, SortedSet<String>> reviewerGroups = reviewBoardConfig.reviewerGroups;
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

    private class ReviewboardUserCompleter extends ImprovedStringsCompleter {

        private Set<String> searchedValues = new HashSet<>();

        ReviewBoard reviewBoard;

        Link usersLink;

        ReviewboardUserCompleter(ReviewBoard reviewBoard, Collection<String> valuesShownWhenNoBuffer) {
            this.reviewBoard = reviewBoard;
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
                if (usersLink == null) {
                    usersLink = reviewBoard.getRootLinkList().getUsersLink();
                }
                List<ReviewUser> users = reviewBoard.searchUsersMatchingText(usersLink, buffer,
                        reviewBoardConfig.searchByUsernamesOnly);
                for (ReviewUser user : users) {
                    values.add(user.toString());
                }
            }

            return super.complete(buffer, cursor, candidates);
        }

        @Override
        protected void addMatchingValueToCandidates(List<CharSequence> candidates, String value) {
            boolean addValue = true;
            for (int i = candidates.size() -1; i >= 0; i--) {
                String candidateAsString = candidates.get(i).toString();
                // prevent dupes of local usernames with reviewboard users
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
            ReviewUser reviewUser = ReviewUser.fromText(value);
            if (reviewUser == null) {
                log.info("Null user for value " + value);
                return super.bufferMatchesValue(buffer, value);
            }
            return super.bufferMatchesValue(buffer, value) || super.bufferMatchesValue(buffer, reviewUser.username)
                    || super.bufferMatchesValue(buffer, reviewUser.fullName())
                    || super.bufferMatchesValue(buffer, reviewUser.firstName) || super.bufferMatchesValue(buffer, reviewUser.lastName);
        }

    }
}
