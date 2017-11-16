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
    public String cannotRunAction() {
        if (config.logLineCount <= 0) {
            return "line count to show (logLineCount) is " + config.logLineCount;
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        if (config.includeRunningBuilds) {
            buildweb.logOutputForBuilds(draft, config.logLineCount, FAILURE, UNSTABLE, BUILDING);
        } else {
            buildweb.logOutputForBuilds(draft, config.logLineCount, FAILURE, UNSTABLE);
        }
    }
}
