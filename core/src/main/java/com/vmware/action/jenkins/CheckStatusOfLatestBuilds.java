package com.vmware.action.jenkins;

import com.vmware.JobBuild;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.JenkinsJobsConfig;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.*;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

@ActionDescription("Checks the status of the latest job specified for the specified user.")
public class CheckStatusOfLatestBuilds extends BaseAction {

    Jenkins jenkins;

    public CheckStatusOfLatestBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void asyncSetup() {
        this.jenkins = serviceLocator.getJenkins();
    }

    @Override
    public void preprocess() {
        this.jenkins.setupAuthenticatedConnection();
    }

    @Override
    public void process() {
        if (StringUtils.isBlank(config.jenkinsJobsToUse)) {
            config.jenkinsJobsToUse = InputUtils.readValueUntilNotBlank("Enter jobs");
        }

        JenkinsJobsConfig jobsConfig = config.getJenkinsJobsConfig();

        for (Job job : jobsConfig.jobs()) {
            checkStatusOfLatestJob(job);
        }
    }

    private void checkStatusOfLatestJob(Job jobToCheck) {
        JobBuildDetails matchedBuild = null;

        log.info("Checking status of job {}", jobToCheck);
        log.debug("Using url {}", jobToCheck.url);
        JobDetails jobDetails = jenkins.getJobDetails(jobToCheck);
        int buildCounter = 0;
        for (JobBuild build : jobDetails.builds) {
            JobBuildDetails buildDetails = jenkins.getJobBuildDetails(build);
            buildCounter++;
            if (buildDetails.getJobInitiator().equals(config.username)) {
                matchedBuild = buildDetails;
                break;
            } else if (buildCounter == config.maxJenkinsBuildsToCheck) {
                log.info("Checked {} number of jenkins builds, set maxJenkinsBuildsToCheck " +
                        "to a higher number if you want more builds to be checked");
                break;
            }
        }
        if (matchedBuild == null) {
            return;
        }
        log.info("Latest build: {}", matchedBuild.url);
        log.info("Result: {}", matchedBuild.realResult());
    }
}
