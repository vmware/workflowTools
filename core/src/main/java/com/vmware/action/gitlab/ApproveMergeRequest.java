package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Approve merge request in Gitlab.")
public class ApproveMergeRequest extends BaseCommitWithMergeRequestAction {
    public ApproveMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Approving merge request {}", draft.mergeRequestUrl());
        gitlab.approveMergeRequest(draft.mergeRequestProjectId(), draft.mergeRequestId());
    }
}
