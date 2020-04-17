package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import static com.vmware.BuildResult.BUILDING;
import static com.vmware.BuildResult.FAILURE;
import static com.vmware.BuildResult.UNSTABLE;

@ActionDescription("Displays build output for jenkins jobs that are not successful.")
public class DisplayBuildOutputForJenkinsJobs extends BaseCommitWithJenkinsBuildsAction {

    public DisplayBuildOutputForJenkinsJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(jenkinsConfig.logLineCount <= 0,
                "line count to show (logLineCount) is " + jenkinsConfig.logLineCount);
    }

    @Override
    public void process() {
        if (jenkinsConfig.includeInProgressBuilds) {
            jenkins.logOutputForBuildsMatchingResult(draft, jenkinsConfig.logLineCount, FAILURE, UNSTABLE, BUILDING);
        } else {
            jenkins.logOutputForBuildsMatchingResult(draft, jenkinsConfig.logLineCount, FAILURE, UNSTABLE);
        }
    }
}
