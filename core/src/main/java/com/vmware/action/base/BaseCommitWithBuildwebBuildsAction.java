package com.vmware.action.base;

import com.vmware.buildweb.Buildweb;
import com.vmware.config.WorkflowConfig;

public abstract class BaseCommitWithBuildwebBuildsAction extends BaseCommitAction {

    protected Buildweb buildweb;

    public BaseCommitWithBuildwebBuildsAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.jobBuildsMatchingUrl(buildwebConfig.buildwebUrl).isEmpty(),
                "the commit has no Buildweb builds in the testing done section");
    }

    @Override
    public void preprocess() {
        buildweb = serviceLocator.getBuildweb();
    }
}
