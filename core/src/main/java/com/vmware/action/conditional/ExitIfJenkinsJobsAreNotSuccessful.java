package com.vmware.action.conditional;

import com.vmware.action.base.AbstractCommitWithBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Reads the testing done section and checks the status for all jenkins jobs found. Exits if any are not successful")
public class ExitIfJenkinsJobsAreNotSuccessful extends AbstractCommitWithBuildsAction {

    public ExitIfJenkinsJobsAreNotSuccessful(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        log.info("");
        jenkins.checkStatusOfJenkinsJobs(draft);
        if (!draft.jenkinsJobsAreSuccessful) {
            log.info("One or more jenkins jobs were not successful!");
            System.exit(0);
        }
    }
}