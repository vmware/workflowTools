package com.vmware.action.jenkins;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.*;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Checks that status of the latest job specified for the specified user.")
public class CheckStatusOfLatestJobs extends BaseAction {

    Jenkins jenkins;

    public CheckStatusOfLatestJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void preprocess() {
        jenkins = serviceLocator.getJenkins();
    }

    @Override
    public void process() {
        String jenkinsJobKeys = config.jenkinsJobKeys;
        if (StringUtils.isBlank(jenkinsJobKeys)) {
            jenkinsJobKeys = InputUtils.readValueUntilNotBlank("Enter job keys");
        }

        String[] jobs = jenkinsJobKeys.trim().split(",");

        for (String job : jobs) {
            checkStatusOfLatestJob(job);
        }
    }

    private void checkStatusOfLatestJob(String job) {
        JobBuildDetails matchedBuild = null;
        if (config.getJenkinsJobValue(job) != null) {
            job = config.jenkinsJobs.get(job).split(",")[0];
        } else {
            log.info("No match for jenkins job key {}, using as job name", job);
        }

        Job jobToCheck = new Job(config.jenkinsUrl, job);
        log.info("Checking status of job {}", job);
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
