package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class BaseCommitWithBuildsAction extends BaseCommitAction {

    protected Jenkins jenkins;

    public BaseCommitWithBuildsAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.jobBuilds.isEmpty()) {
            return "the commit has no job builds in the testing done section";
        } else {
            return super.cannotRunAction();
        }
    }

    @Override
    public void preprocess() {
        this.jenkins = serviceLocator.getJenkins();
    }
}
