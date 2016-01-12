
package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Reads the testing done section and checks the status for all jenkins jobs found.")
public class CheckStatusOfJenkinsJobs extends BaseCommitWithBuildsAction {

    public CheckStatusOfJenkinsJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        jenkins.checkStatusOfJenkinsJobs(draft);
    }
}
