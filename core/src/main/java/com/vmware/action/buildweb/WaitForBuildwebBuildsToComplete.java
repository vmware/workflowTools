package com.vmware.action.buildweb;

import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.ThreadUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@ActionDescription("Waits the configured amount of time for all buildweb builds in the commit to complete.")
public class WaitForBuildwebBuildsToComplete extends BaseCommitWithBuildwebBuildsAction {
    public WaitForBuildwebBuildsToComplete(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(draft.jobBuildsMatchingUrl(buildwebConfig.buildwebUrl).isEmpty(), "commit does not contain any buildweb builds");
    }

    @Override
    public void process() {
        Callable<Boolean> condition = () -> {
            buildweb.checkStatusOfBuilds(draft);
            return draft.allJobBuildsMatchingUrlAreComplete(buildwebConfig.buildwebUrl);
        };
        log.info("Waiting for all buildweb builds to complete");
        ThreadUtils.sleepUntilCallableReturnsTrue(condition, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        log.info("All buildweb builds have completed");
    }
}
