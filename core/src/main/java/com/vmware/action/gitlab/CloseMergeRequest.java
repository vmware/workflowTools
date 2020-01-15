package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;

@ActionDescription("Close the merge request matching the commit.")
public class CloseMergeRequest extends BaseCommitWithMergeRequestAction {

    public CloseMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Closing merge request {}", draft.mergeRequestUrl());
        MergeRequest mergeRequest = gitlab.getMergeRequest(draft.mergeRequestProjectId(), draft.mergeRequestId());
        mergeRequest.stateEvent = "close";
        gitlab.updateMergeRequest(mergeRequest);
    }
}
