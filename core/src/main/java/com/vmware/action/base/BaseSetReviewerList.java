package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.reviewboard.domain.Link;
import com.vmware.reviewboard.domain.ReviewUser;
import com.vmware.util.collection.OverwritableSet;
import com.vmware.util.input.CommaArgumentDelimeter;
import com.vmware.util.input.ImprovedStringsCompleter;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.vmware.util.StringUtils.isInteger;

public abstract class BaseSetReviewerList extends BaseCommitReadAction {
    private boolean searchReviewBoardForUsers;
    private boolean addToReviewerList;
    private ReviewBoard reviewboard;

    public BaseSetReviewerList(WorkflowConfig config, boolean searchReviewBoardForUsers, boolean addToReviewerList) {
        super(config, "reviewedBy");
        this.searchReviewBoardForUsers = searchReviewBoardForUsers;
        this.addToReviewerList = addToReviewerList;
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
            this.reviewboard.setupAuthenticatedConnectionWithLocalTimezone(config.reviewBoardDateFormat);
        }
    }

    @Override
    public void process() {
        Set<String> autocompleteOptions = new HashSet<>();
        if (config.reviewerGroups != null && !config.reviewerGroups.isEmpty()) {
            autocompleteOptions.addAll(config.reviewerGroups.keySet());
            for (String groupName : config.reviewerGroups.keySet()) {
                autocompleteOptions.addAll(config.reviewerGroups.get(groupName));
            }
            log.info("Enter group name or list number as a reviewer to add entire review group");
            int count = 1;
            for (String reviewerGroupName : config.reviewerGroups.keySet()) {
                log.info("[{}] {}: {}",count++,reviewerGroupName, config.reviewerGroups.get(reviewerGroupName).toString());
            }
        } else {
            log.info("Reviewer groups can be added by setting the reviewerGroups property in an external config file");
        }
        if (draft.hasReviewers()) {
            log.info("Existing reviewer list: {}", draft.reviewedBy);
        }

        if (autocompleteOptions.size() > 0) {
            log.info("Tab can be used to autocomplete names");
        }

        Completer userCompleter = createUserCompleter(autocompleteOptions);
        ArgumentCompleter argumentCompleter = new ArgumentCompleter(new CommaArgumentDelimeter(), userCompleter);
        argumentCompleter.setStrict(false);

        if (searchReviewBoardForUsers) {
            log.info("Individual reviewers are parsed from review groups, after entering 3 characters, reviewboard is searched for a user");
        }
        String reviewers = InputUtils.readValue("Reviewers (blank means no reviewer)", argumentCompleter);
        String existingReviewers = addToReviewerList && StringUtils.isNotBlank(draft.reviewedBy) ? draft.reviewedBy + "," : "";
        draft.reviewedBy = generateReviewerList(existingReviewers + reviewers);
        log.info("Reviewer list for review: {}", draft.reviewedBy);
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

    private String generateReviewerList(String reviewersText) {
        if (reviewersText.trim().isEmpty()) {
            return config.trivialReviewerLabel;
        }

        Collection<String> parsedReviewers = new OverwritableSet.UniqueArrayList<>();
        String[] reviewers = reviewersText.split(",");
        for (String reviewer : reviewers) {
            String fragment = reviewer.trim();
            if (fragment.isEmpty()) {
                continue;
            }
            Collection<String> reviewersInGroup = getReviewersInGroup(fragment);
            if (reviewersInGroup != null) {
                addReviewers(parsedReviewers, reviewersInGroup);
                continue;
            }

            fragment = convertFragmentToReviewerUsername(fragment);
            addReviewer(parsedReviewers, fragment);
        }
        return StringUtils.join(parsedReviewers);
    }

    private String convertFragmentToReviewerUsername(String fragment) {
        if (!fragment.contains("(")) {
            return fragment;
        }
        return fragment.substring(0, fragment.indexOf("(")).trim();
    }

    private Set<String> getReviewersInGroup(String fragment) {
        LinkedHashMap<String, SortedSet<String>> reviewerGroups = config.reviewerGroups;
        if (reviewerGroups == null) {
            return null;
        }

        int possibleIndexNumber = isInteger(fragment) ? Integer.parseInt(fragment) : -1;
        int count = 1;
        for (String groupName : reviewerGroups.keySet()) {
            if (groupName.startsWith(fragment) || possibleIndexNumber == count++) {
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
            if (StringUtils.isBlank(buffer)) {
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
                List<ReviewUser> users = reviewBoard.searchUsersMatchingText(usersLink, buffer, config.searchByUsernamesOnly);
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
                System.out.println("Null user for value " + value);
                return super.bufferMatchesValue(buffer, value);
            }
            return super.bufferMatchesValue(buffer, value) || super.bufferMatchesValue(buffer, reviewUser.username)
                    || super.bufferMatchesValue(buffer, reviewUser.fullName())
                    || super.bufferMatchesValue(buffer, reviewUser.firstName) || super.bufferMatchesValue(buffer, reviewUser.lastName);
        }

    }
}
