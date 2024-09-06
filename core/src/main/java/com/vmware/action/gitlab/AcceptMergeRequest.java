package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;

@ActionDescription("Accept merge request in Gitlab so that it is merged to the target branch.")
public class AcceptMergeRequest extends BaseCommitWithMergeRequestAction {
    public AcceptMergeRequest(WorkflowConfig config) {
        super(config, true);
    }

    @Override
    public void process() {
        log.info("Accepting merge request {}", draft.requestUrl);
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        gitlab.acceptMergeRequest(mergeRequest);
    }
}
