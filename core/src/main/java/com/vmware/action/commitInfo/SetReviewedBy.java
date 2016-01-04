package com.vmware.action.commitInfo;

import com.vmware.action.base.AbstractSetReviewerList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Sets the reviewed by section. Reads list of reviewers from the property targetReviewers in the config file." +
        "\nReviewer groups can also be specified by setting the reviewerGroups property in the config file.")
public class SetReviewedBy extends AbstractSetReviewerList {

    public SetReviewedBy(WorkflowConfig config) throws NoSuchFieldException {
        super(config, false);
    }
}
