package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseCommitWithJenkinsBuildsAction extends BaseVappAction {

    protected Jenkins jenkins;

    public BaseCommitWithJenkinsBuildsAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl).isEmpty()) {
            return "the commit has no Jenkins job builds in the testing done section";
        } else {
            return super.cannotRunAction();
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
