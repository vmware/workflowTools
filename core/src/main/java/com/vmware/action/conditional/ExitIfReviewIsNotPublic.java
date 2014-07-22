/*
 * Project Horizon
 * (c) 2014 VMware, Inc. All rights reserved.
 * VMware Confidential.
 */
package com.vmware.action.conditional;

import com.vmware.action.base.AbstractCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Exits if a review request has not been made public")
public class ExitIfReviewIsNotPublic extends AbstractCommitWithReviewAction {

    public ExitIfReviewIsNotPublic(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        if (draft.reviewRequest.isPublic) {
            return;
        }

        log.info("");
        log.info("Exiting as the review is not public");
        System.exit(0);
    }
}
