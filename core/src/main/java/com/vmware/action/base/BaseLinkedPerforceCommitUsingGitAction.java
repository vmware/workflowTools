package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

public abstract class BaseLinkedPerforceCommitUsingGitAction extends BasePerforceCommitUsingGitAction {

    protected BaseLinkedPerforceCommitUsingGitAction(WorkflowConfig config) {
        super(config);
        super.failIfCannotBeRun = false;
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isEmpty(draft.perforceChangelistId)) {
            return "no changelist id read for commit";
        }
        return super.cannotRunAction();
    }
}
