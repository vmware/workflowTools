package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static com.vmware.utils.StringUtils.isInteger;

public abstract class AbstractSetReviewerList extends AbstractCommitReadAction {
    protected boolean addToReviewerList;


    public AbstractSetReviewerList(WorkflowConfig config, boolean addToReviewerList) throws NoSuchFieldException {
        super(config, "reviewedBy");
        this.addToReviewerList = addToReviewerList;
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        List<String> autocompleteOptions = new ArrayList<String>();
        if (config.targetReviewers == null) {
            log.info("No targetReviewers configured, add to external config file if wanted");
        } else {
            autocompleteOptions.addAll(config.targetReviewers);
            log.info("Using reviewer list {}", config.targetReviewers.toString());
        }
        if (config.reviewerGroups != null && !config.reviewerGroups.isEmpty()) {
            autocompleteOptions.addAll(config.reviewerGroups.keySet());
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

        String reviewers = InputUtils.readValue("Reviewers (blank means no reviewer)", autocompleteOptions);
        String existingReviewers = addToReviewerList && StringUtils.isNotBlank(draft.reviewedBy) ? draft.reviewedBy + "," : "";
        draft.reviewedBy = generateReviewerList(existingReviewers + reviewers);
        log.info("Reviewer list for review: {}", draft.reviewedBy);
    }

    private String generateReviewerList(String reviewersText) {
        if (reviewersText.trim().isEmpty()) {
            return config.trivialReviewerLabel;
        }

        List<String> parsedReviewers = new ArrayList<String>();
        String[] reviewers = reviewersText.split(",");
        for (String reviewer : reviewers) {
            String fragment = reviewer.trim();
            if (fragment.isEmpty()) {
                continue;
            }
            int possibleIndexNumber = isInteger(fragment) ? Integer.parseInt(fragment) : -1;
            boolean foundGroup = false;
            LinkedHashMap<String, SortedSet<String>> reviewerGroups = config.reviewerGroups;
            if (reviewerGroups != null) {
                int count = 1;
                for (String groupName : reviewerGroups.keySet()) {
                    if (groupName.startsWith(fragment) || possibleIndexNumber == count++) {
                        foundGroup = true;
                        Set<String> reviewersToAdd = reviewerGroups.get(groupName);
                        for (String reviewerToAdd : reviewersToAdd) {
                            addReviewer(parsedReviewers, reviewerToAdd);
                        }
                        break;
                    }
                }
            }
            if (foundGroup) {
                continue;
            }

            if (config.targetReviewers != null) {
                for (String existingReviewer : config.targetReviewers) {
                    if (existingReviewer.startsWith(fragment)) {
                        fragment = existingReviewer;
                        break;
                    }
                }
            }
            addReviewer(parsedReviewers, fragment);
        }
        return StringUtils.join(parsedReviewers);
    }

    private void addReviewer(List<String> parsedReviewers, String fragment) {
        if (fragment.equals(config.username)) {
            log.debug("Not adding myself ({}) to reviewer list", config.username);
            return;
        }
        if (parsedReviewers.contains(fragment)) {
            log.debug("Already have added {} as a reviewer so skipping", fragment);
            return;
        }
        parsedReviewers.add(fragment);
    }
}
