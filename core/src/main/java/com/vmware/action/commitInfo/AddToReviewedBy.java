package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseSetReviewerList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Adds to the reviewed by section. Reads list of reviewers from the property targetReviewers in the config file.")
public class AddToReviewedBy extends BaseSetReviewerList {

    public AddToReviewedBy(WorkflowConfig config) throws NoSuchFieldException {
        super(config, true);
    }
}
