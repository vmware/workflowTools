package com.vmware.action.buildweb;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Tails the output for buildweb failures.")
public class DisplayBuildOutputForBuildwebFailures extends BaseCommitWithBuildwebBuildsAction {
    public DisplayBuildOutputForBuildwebFailures(WorkflowConfig config) {
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
        buildweb.logOutputForFailedBuilds(draft, config.logLineCount);
    }
}
