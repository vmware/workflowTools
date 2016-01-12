package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.UrlUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays the current commit's review url on the command line.")
public class DisplayReviewUrl extends BaseCommitWithReviewAction {

    public DisplayReviewUrl(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("{}r/{}/", UrlUtils.addTrailingSlash(config.reviewboardUrl), draft.id);
    }
}
