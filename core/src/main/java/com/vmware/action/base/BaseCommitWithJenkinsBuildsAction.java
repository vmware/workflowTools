package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;

public abstract class BaseCommitWithJenkinsBuildsAction extends BaseVappAction {

    protected Jenkins jenkins;
    protected boolean continueIfNoBuilds;

    public BaseCommitWithJenkinsBuildsAction(WorkflowConfig config) {
        super(config);
    }

    public BaseCommitWithJenkinsBuildsAction(WorkflowConfig config, boolean continueIfNoBuilds) {
        super(config);
        this.continueIfNoBuilds = continueIfNoBuilds;
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (!continueIfNoBuilds) {
            super.skipActionIfTrue(draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl).isEmpty(),
                    "the commit has no Jenkins job builds in the testing done section");
        }
    }

    @Override
    public void asyncSetup() {
        this.jenkins = serviceLocator.getJenkins();
    }

    @Override
    public void preprocess() {
        this.jenkins.setupAuthenticatedConnection();
    }
}
