package com.vmware.action.review;

import com.vmware.action.base.BaseCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.logging.LogLevel;

import java.io.File;

import static java.lang.String.format;

@ActionDescription("Uses rbt post to upload a diff to reviewboard.")
public class UploadReviewDiffWithRbt extends UploadReviewDiff {

    public UploadReviewDiffWithRbt(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("rbt");
    }

    @Override
    protected void uploadReviewDiff() {
        File workingDirectory = new File(System.getProperty("user.dir"));
        uploadDiffUsingRbt(workingDirectory, null); // don't specify changelist id as working with git repo
    }
}
