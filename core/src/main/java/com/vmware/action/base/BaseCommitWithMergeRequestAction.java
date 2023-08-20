package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithMergeRequestAction extends BaseCommitUsingGitlabAction {
    private boolean loadMergeRequest;
    private boolean skipIfNoMergeRequest;

    public BaseCommitWithMergeRequestAction(WorkflowConfig config) {
        this(config, false, false);
    }

    public BaseCommitWithMergeRequestAction(WorkflowConfig config, boolean loadMergeRequest) {
        this(config, loadMergeRequest, false);
    }


    public BaseCommitWithMergeRequestAction(WorkflowConfig config, boolean loadMergeRequest, boolean skipIfNoMergeRequest) {
        super(config);
        this.loadMergeRequest = loadMergeRequest;
        this.skipIfNoMergeRequest = skipIfNoMergeRequest;
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (!skipIfNoMergeRequest) {
            super.failIfTrue(!draft.hasMergeRequest(), "no git lab merge request associated with commit");
        }
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (skipIfNoMergeRequest) {
            super.skipActionIfTrue(!draft.hasMergeRequest(), "no git lab merge request associated with commit");
        }
    }

    @Override
    public void preprocess() {
        super.preprocess();
        if (loadMergeRequest && draft.getGitlabMergeRequest() == null) {
            draft.setGitlabMergeRequest(gitlab.getMergeRequest(gitlabConfig.gitlabProjectId, draft.mergeRequestId()));
        }
    }
}
