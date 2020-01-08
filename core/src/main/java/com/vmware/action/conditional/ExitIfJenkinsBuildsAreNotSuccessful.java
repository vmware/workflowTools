package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Reads the testing done section and checks the status for all jenkins builds found. Exits if any are not successful.")
public class ExitIfJenkinsBuildsAreNotSuccessful extends BaseCommitWithJenkinsBuildsAction {

    public ExitIfJenkinsBuildsAreNotSuccessful(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (draft.jenkinsBuildsAreSuccessful == null) {
            log.info("");
            jenkins.checkStatusOfBuilds(draft);
            if (!draft.jenkinsBuildsAreSuccessful) {
                cancelWithMessage("one or more jenkins jobs were not successful");
            }
        } else if (!draft.jenkinsBuildsAreSuccessful) {
            cancelWithMessage("one or more jenkins jobs were not successful");
        }
    }
}