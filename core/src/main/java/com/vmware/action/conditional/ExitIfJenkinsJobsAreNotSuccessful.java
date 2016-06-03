package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Reads the testing done section and checks the status for all jenkins jobs found. Exits if any are not successful.")
public class ExitIfJenkinsJobsAreNotSuccessful extends BaseCommitWithJenkinsBuildsAction {

    public ExitIfJenkinsJobsAreNotSuccessful(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("");
        jenkins.checkStatusOfJenkinsJobs(draft);
        if (!draft.jenkinsJobsAreSuccessful) {
            log.info("One or more jenkins jobs were not successful!");
            System.exit(0);
        }
    }
}