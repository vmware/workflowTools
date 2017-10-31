package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseSetReviewersList extends BaseSetUsersList {

    public BaseSetReviewersList(WorkflowConfig config, boolean searchReviewBoardForUsers, boolean addToReviewerList) {
        super(config, "reviewedBy", searchReviewBoardForUsers, addToReviewerList);
    }

    @Override
    public void process() {
        Set<String> autocompleteOptions = new HashSet<>();
        if (reviewBoardConfig.reviewerGroups != null && !reviewBoardConfig.reviewerGroups.isEmpty()) {
            autocompleteOptions.addAll(reviewBoardConfig.reviewerGroups.keySet());
            for (String groupName : reviewBoardConfig.reviewerGroups.keySet()) {
                autocompleteOptions.addAll(reviewBoardConfig.reviewerGroups.get(groupName));
            }
            log.info("Enter group name or list number as a reviewer to add entire review group");
            int count = 1;
            for (String reviewerGroupName : reviewBoardConfig.reviewerGroups.keySet()) {
                log.info("[{}] {}: {}",count++,reviewerGroupName, reviewBoardConfig.reviewerGroups.get(reviewerGroupName).toString());
            }
        } else {
            log.info("Reviewer groups can be added by setting the reviewerGroups property in an external config file");
        }
        if (draft.hasReviewers()) {
            log.info("Existing reviewer list: {}", draft.reviewedBy);
        }

        draft.reviewedBy = readUsers(autocompleteOptions, draft.reviewedBy, "Reviewers (blank means no reviewer)");
        log.info("Reviewer list: {}", draft.reviewedBy);

        if (reviewBoardConfig.alwaysIncludeReviewUrl && commitConfig.trivialReviewerLabel.equals(draft.reviewedBy)) {
            log.info("Setting review url to {} as it is a trivial review", commitConfig.noReviewNumberLabel);
            draft.id = commitConfig.noReviewNumberLabel;
        }
    }

}
