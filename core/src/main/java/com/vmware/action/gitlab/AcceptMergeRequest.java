package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.util.exception.FatalException;

@ActionDescription("Accept merge request in Gitlab so that it is merged to the target branch.")
public class AcceptMergeRequest extends BaseCommitWithMergeRequestAction {
    public AcceptMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Accepting merge request {}", draft.mergeRequestUrl());
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        if (!mergeRequest.canBeMerged()) {
            throw new FatalException("Cannot accept merge request {} as it has a status of {} and cannot be merged",
                    mergeRequest.iid, mergeRequest.mergeStatus);
        }
        gitlab.acceptMergeRequest(mergeRequest);
    }
}
