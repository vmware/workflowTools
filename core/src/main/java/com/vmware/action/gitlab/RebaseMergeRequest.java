package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Rebase merge request via Gitlab.")
public class RebaseMergeRequest extends BaseCommitWithMergeRequestAction {
    public RebaseMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Rebasing merge request {}", draft.gitlabMergeRequestId);
        gitlab.rebaseMergeRequest(gitlabConfig.gitlabProjectId, draft.gitlabMergeRequestId);
    }
}
