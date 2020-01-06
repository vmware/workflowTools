package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.UrlUtils;

@ActionDescription("Copies the review board url to the clipboard. Handy for pasting it into a browser.")
public class CopyReviewUrlToClipboard extends BaseCommitUsingReviewBoardAction {
    public CopyReviewUrlToClipboard(WorkflowConfig config) {
        super(config);
    }


    @Override
    public void process() {
        String reviewUrl = String.format("%sr/%s/", UrlUtils.addTrailingSlash(reviewBoardConfig.reviewboardUrl), draft.id);

        log.info("Copying review url {} to clipboard", reviewUrl);
        SystemUtils.copyTextToClipboard(reviewUrl);
    }
}
