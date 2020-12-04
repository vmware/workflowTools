package com.vmware.action.jenkins;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.Job;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

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
        if (StringUtils.isEmpty(jenkinsConfig.jenkinsJobsToUse)) {
            jenkinsConfig.jenkinsJobsToUse = InputUtils.readValueUntilNotBlank("Enter jobs");
        }

        config.getJenkinsJobsConfig().jobs().forEach(this::checkStatusOfLatestJob);
    }

    private void checkStatusOfLatestJob(Job jobToCheck) {
        JobBuild matchedBuild = null;

        log.info("Checking status of job {}", jobToCheck);
        log.debug("Using url {}", jobToCheck.url);
        Job job = jenkins.getJobDetails(jobToCheck);
        int buildCounter = 0;
        for (JobBuild build : job.builds) {
            JobBuild buildDetails = jenkins.getJobBuildDetails(build);
            buildCounter++;
            if (buildDetails.getJobInitiator().equals(config.username)) {
                matchedBuild = buildDetails;
                break;
            } else if (buildCounter == jenkinsConfig.maxJenkinsBuildsToCheck) {
                log.info("Checked {} number of jenkins builds, set maxJenkinsBuildsToCheck " +
                        "to a higher number if you want more builds to be checked", buildCounter);
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
