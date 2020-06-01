package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Removes the review url line from the commit info.")
public class RemoveReviewUrl extends BaseCommitAction {
    public RemoveReviewUrl(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (StringUtils.isEmpty(draft.id)) {
            skipActionDueTo("no review url found in commit");
        }
    }

    @Override
    public void process() {
        if (reviewBoardConfig.alwaysIncludeReviewUrl) {
            log.info("Setting review url to {}", commitConfig.noReviewNumberLabel);
            draft.id = commitConfig.noReviewNumberLabel;
        } else {
            log.info("Removing review url for review {} from commit", draft.id);
            draft.id = null;
        }
    }
}
