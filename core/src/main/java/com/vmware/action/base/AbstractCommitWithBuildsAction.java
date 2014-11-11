package com.vmware.action.base;

import com.vmware.ServiceLocator;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class AbstractCommitWithBuildsAction extends AbstractCommitAction {

    protected Jenkins jenkins;

    public AbstractCommitWithBuildsAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException {
        if (draft.jobBuilds.isEmpty()) {
            log.info("Ignoring action {} as the commit has no job builds in the testing done section", this.getClass().getSimpleName());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void preprocess() throws IOException, URISyntaxException, IllegalAccessException {
        this.jenkins = ServiceLocator.getJenkins(config.jenkinsUrl, config.username, config.jenkinsUsesCsrf);
    }
}
