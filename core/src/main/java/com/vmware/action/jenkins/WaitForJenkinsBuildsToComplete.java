package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.ThreadUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@ActionDescription("Waits the configured amount of time for all jenkins builds in the commit to complete.")
public class WaitForJenkinsBuildsToComplete extends BaseCommitWithJenkinsBuildsAction {
    public WaitForJenkinsBuildsToComplete(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.jobBuildsMatchingUrl(config.jenkinsUrl).isEmpty()) {
            return "commit does not contain any jenkins builds";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        Callable<Boolean> condition = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                jenkins.checkStatusOfBuilds(draft);
                return draft.allJobBuildsMatchingUrlAreComplete(config.jenkinsUrl);
            }
        };
        log.info("Waiting for all jenkins builds to complete");
        ThreadUtils.sleepUntilCallableReturnsTrue(condition, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);
        log.info("All jenkins builds have completed");
    }
}
