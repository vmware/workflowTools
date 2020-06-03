package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithMergeRequestAction extends BaseCommitUsingGitlabAction {
    private boolean loadMergeRequest = false;

    public BaseCommitWithMergeRequestAction(WorkflowConfig config) {
        this(config, false);
    }

    public BaseCommitWithMergeRequestAction(WorkflowConfig config, boolean loadMergeRequest) {
        super(config);
        this.loadMergeRequest = loadMergeRequest;
    }

    @Override
    public void preprocess() {
        super.preprocess();
        if (loadMergeRequest && draft.getGitlabMergeRequest() == null) {
            draft.setGitlabMergeRequest(gitlab.getMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId()));
        }
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        super.failIfTrue(!draft.hasMergeRequest(), "no git lab merge request associated with commit");
    }
}
