package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Accept merge request in Gitlab so that it is merged to the target branch.")
public class AcceptMergeRequest extends BaseCommitWithMergeRequestAction {
    public AcceptMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Accepting merge request {}", draft.gitlabMergeRequestId);
        gitlab.acceptMergeRequest(gitlabConfig.gitlabProjectId, draft.gitlabMergeRequestId);
    }
}
