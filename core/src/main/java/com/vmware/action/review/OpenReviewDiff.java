package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.BrowserUtils;
import com.vmware.util.UrlUtils;

@ActionDescription("Opens the review diff page in the default web browser")
public class OpenReviewDiff extends BaseCommitWithReviewAction {

    public OpenReviewDiff(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String reviewUrl = UrlUtils.addRelativePaths(commitConfig.getReviewboardUrl(), "r", draft.id, "diff");
        BrowserUtils.openUrl(reviewUrl);
    }
}
