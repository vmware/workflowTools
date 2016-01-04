package com.vmware.action.review;

import com.vmware.action.base.AbstractCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.UrlUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Displays the current commit's review url on the command line.")
public class DisplayReviewUrl extends AbstractCommitWithReviewAction {

    public DisplayReviewUrl(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        log.info("{}r/{}/", UrlUtils.addTrailingSlash(config.reviewboardUrl), draft.id);
    }
}
