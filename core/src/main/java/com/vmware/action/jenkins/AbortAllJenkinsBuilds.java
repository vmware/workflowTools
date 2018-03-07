package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Aborts all jenkins builds specified in the testing done section.")
public class AbortAllJenkinsBuilds extends BaseCommitWithJenkinsBuildsAction {

    public AbortAllJenkinsBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        jenkins.checkStatusOfBuilds(draft);
        jenkins.abortAllRunningBuilds(draft);
    }
}
