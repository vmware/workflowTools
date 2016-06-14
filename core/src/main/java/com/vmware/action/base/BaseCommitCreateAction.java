package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;

/**
 * Common functionality for actions that create a git commit.
 */
public abstract class BaseCommitCreateAction extends BaseCommitAction {
    public BaseCommitCreateAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!draft.hasData()) {
            return "there no information set for the commit message";
        }
        return super.cannotRunAction();
    }
}
