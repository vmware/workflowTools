package com.vmware.action.review;

import java.io.File;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.reviewboard.domain.RepoType;

@ActionDescription("Uses rbt post to upload a diff to reviewboard.")
public class UploadReviewDiffWithRbt extends UploadReviewDiff {

    public UploadReviewDiffWithRbt(WorkflowConfig config) {
        super(config);
        super.setExpectedCommandsToBeAvailable("rbt");
    }

    @Override
    protected void uploadReviewDiff() {
        RepoType repoType = draft.repoType;
        File workingDirectory = new File(System.getProperty("user.dir"));
        if (repoType == RepoType.perforce){
            String changelistId = determineChangelistIdToUse();
            uploadDiffUsingRbt(workingDirectory, changelistId);
        } else {
            // don't specify changelist id as working with non perforce repo
            uploadDiffUsingRbt(workingDirectory, null);
        }
    }
}
