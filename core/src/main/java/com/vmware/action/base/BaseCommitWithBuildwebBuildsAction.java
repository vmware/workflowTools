package com.vmware.action.base;

import com.vmware.ServiceLocator;
import com.vmware.buildweb.Buildweb;
import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithBuildwebBuildsAction extends BaseCommitAction {

    protected Buildweb buildweb;

    @Override
    public String cannotRunAction() {
        if (draft.jobBuildsMatchingUrl(config.buildwebUrl).isEmpty()) {
            return "the commit has no Buildweb builds in the testing done section";
        } else {
            return super.cannotRunAction();
        }
    }

    public BaseCommitWithBuildwebBuildsAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        buildweb = serviceLocator.getBuildweb();
    }
}
