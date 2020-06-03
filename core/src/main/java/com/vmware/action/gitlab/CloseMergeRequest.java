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
        log.info("Closing merge request {}", draft.mergeRequestUrl);
        if (draft.getGitlabMergeRequest() == null) {
            draft.setGitlabMergeRequest(gitlab.getMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId()));
        }
        MergeRequest mergeRequest = draft.getGitlabMergeRequest();
        mergeRequest.stateEvent = "close";
        gitlab.updateMergeRequest(mergeRequest);
    }
}
