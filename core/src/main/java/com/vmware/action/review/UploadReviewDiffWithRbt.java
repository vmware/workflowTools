package com.vmware.action.review;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

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
        String repoType = draft.repoType;
        File workingDirectory = new File(System.getProperty("user.dir"));
        if (repoType.contains("perforce")){
            String changelistId = determineChangelistIdToUse();
            uploadDiffUsingRbt(workingDirectory, changelistId);
        } else {
            // don't specify changelist id as working with non perforce repo
            uploadDiffUsingRbt(workingDirectory, null);
        }
    }
}
