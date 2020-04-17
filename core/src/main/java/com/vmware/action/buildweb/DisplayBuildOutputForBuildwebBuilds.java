package com.vmware.action.buildweb;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import static com.vmware.BuildResult.BUILDING;
import static com.vmware.BuildResult.FAILURE;
import static com.vmware.BuildResult.UNSTABLE;

@ActionDescription("Tails the output for buildweb builds that are not successful.")
public class DisplayBuildOutputForBuildwebBuilds extends BaseCommitWithBuildwebBuildsAction {
    public DisplayBuildOutputForBuildwebBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        skipActionIfTrue(buildwebConfig.logLineCount <= 0, "line count to show (logLineCount) is " + buildwebConfig.logLineCount);
    }

    @Override
    public void process() {
        if (buildwebConfig.includeInProgressBuilds) {
            buildweb.logOutputForBuilds(draft, buildwebConfig.logLineCount, FAILURE, UNSTABLE, BUILDING);
        } else {
            buildweb.logOutputForBuilds(draft, buildwebConfig.logLineCount, FAILURE, UNSTABLE);
        }
    }
}
