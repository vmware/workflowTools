package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.BrowserUtils;
import com.vmware.util.UrlUtils;

@ActionDescription("Opens the review url in the default web browser")
public class OpenReviewUrl extends BaseCommitWithReviewAction {

    public OpenReviewUrl(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String reviewUrl = UrlUtils.addRelativePaths(commitConfig.getReviewboardUrl(), "r", draft.id);
        BrowserUtils.openUrl(reviewUrl);
    }
}
