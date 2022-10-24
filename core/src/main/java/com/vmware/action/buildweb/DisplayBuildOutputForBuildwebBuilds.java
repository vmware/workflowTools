package com.vmware.action.buildweb;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

import static com.vmware.BuildStatus.BUILDING;
import static com.vmware.BuildStatus.FAILURE;
import static com.vmware.BuildStatus.UNSTABLE;

@ActionDescription("Tails the output for buildweb builds that are not successful.")
public class DisplayBuildOutputForBuildwebBuilds extends BaseCommitWithBuildwebBuildsAction {
    public DisplayBuildOutputForBuildwebBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        if (StringUtils.isEmpty(buildwebConfig.sandboxBuildNumber)) {
            super.checkIfActionShouldBeSkipped();
        }
        skipActionIfTrue(buildwebConfig.logLineCount <= 0, "line count to show (logLineCount) is " + buildwebConfig.logLineCount);
    }

    @Override
    public void process() {
        if (StringUtils.isNotBlank(buildwebConfig.sandboxBuildNumber)) {
            Padder buildPadder = new Padder("Buildweb build {}", buildwebConfig.sandboxBuildNumber);
            buildPadder.infoTitle();
            log.info(buildweb.getBuildOutput(buildwebConfig.sandboxBuildNumber, buildwebConfig.logLineCount));
            buildPadder.infoTitle();
        } else if (buildwebConfig.includeInProgressBuilds) {
            buildweb.logOutputForBuilds(draft, buildwebConfig.logLineCount, FAILURE, UNSTABLE, BUILDING);
        } else {
            buildweb.logOutputForBuilds(draft, buildwebConfig.logLineCount, FAILURE, UNSTABLE);
        }
    }
}
