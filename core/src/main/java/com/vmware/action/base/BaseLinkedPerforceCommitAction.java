package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

public abstract class BaseLinkedPerforceCommitAction extends BasePerforceCommitAction {

    public BaseLinkedPerforceCommitAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!git.workingDirectoryIsInGitRepo()) {
            return "not in git repo directory";
        }
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            return "no changelist id read for commit";
        }
        return super.cannotRunAction();
    }
}
