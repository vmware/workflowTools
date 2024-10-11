package com.vmware.action.gitlab;

import com.vmware.action.base.BaseSetReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Sets the reviewed by section. Reads reviewers from gitlab, review groups can also be configured by setting the property reviewerGroups.")
public class SetReviewedByUsingGitlab extends BaseSetReviewersList {

    public SetReviewedByUsingGitlab(WorkflowConfig config) {
        super(config, CandidateSearchType.gitlab, false);
    }
}