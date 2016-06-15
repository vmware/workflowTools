package com.vmware.action.base;

import com.vmware.ServiceLocator;
import com.vmware.buildweb.Buildweb;
import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithBuildwebBuildsAction extends BaseCommitAction {

    protected Buildweb buildweb;

    public BaseCommitWithBuildwebBuildsAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        buildweb = serviceLocator.getBuildweb();
    }
}
