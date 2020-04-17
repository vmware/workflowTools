package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

import static com.vmware.util.StringUtils.isEmpty;

public abstract class BaseLinkedPerforceCommitUsingGitAction extends BasePerforceCommitUsingGitAction {

    protected BaseLinkedPerforceCommitUsingGitAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(isEmpty(draft.perforceChangelistId), "no changelist id read for commit");
    }
}
