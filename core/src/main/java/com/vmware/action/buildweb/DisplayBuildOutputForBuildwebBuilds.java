package com.vmware.action.buildweb;

import static com.vmware.BuildResult.BUILDING;
import static com.vmware.BuildResult.FAILURE;
import static com.vmware.BuildResult.UNSTABLE;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Tails the output for buildweb builds that are not successful.")
public class DisplayBuildOutputForBuildwebBuilds extends BaseCommitWithBuildwebBuildsAction {
    public DisplayBuildOutputForBuildwebBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (buildwebConfig.logLineCount <= 0) {
            return "line count to show (logLineCount) is " + buildwebConfig.logLineCount;
        }
        return super.cannotRunAction();
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
