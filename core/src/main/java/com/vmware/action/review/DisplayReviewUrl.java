package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

@ActionDescription("Displays the current commit's review url on the command line.")
public class DisplayReviewUrl extends BaseCommitWithReviewAction {

    public DisplayReviewUrl(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isInteger(draft.id)) {
            log.info("{}r/{}/", UrlUtils.addTrailingSlash(reviewBoardConfig.reviewboardUrl), draft.id);
        } else {
            log.info("Review url: {}", draft.id);
        }
    }
}
