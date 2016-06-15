
package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Reads the testing done section and checks the status for all jenkins builds found.")
public class CheckStatusOfJenkinsBuilds extends BaseCommitWithJenkinsBuildsAction {

    public CheckStatusOfJenkinsBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        jenkins.checkStatusOfBuilds(draft);
    }
}
