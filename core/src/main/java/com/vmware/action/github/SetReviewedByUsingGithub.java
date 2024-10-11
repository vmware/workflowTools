package com.vmware.action.github;

import com.vmware.action.base.BaseSetReviewersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Sets the reviewed by section. Reads reviewers from github, review groups can also be configured by setting the property reviewerGroups.")
public class SetReviewedByUsingGithub extends BaseSetReviewersList {

    public SetReviewedByUsingGithub(WorkflowConfig config) {
        super(config, CandidateSearchType.github, false);
    }
}