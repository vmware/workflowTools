package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Displays build output for jenkins jobs that are not successful")
public class DisplayBuildOutputForJenkinsFailures extends BaseCommitWithJenkinsBuildsAction {

    public DisplayBuildOutputForJenkinsFailures(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (config.logLineCount <= 0) {
            return "line count to show (logLineCount) is " + config.logLineCount;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        jenkins.logOutputForFailedBuilds(draft, config.logLineCount);
    }
}
