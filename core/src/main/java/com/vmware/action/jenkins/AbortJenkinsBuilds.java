package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.JobBuild;
import com.vmware.BuildResult;
import com.vmware.http.exception.NotFoundException;
import com.vmware.config.jenkins.Job;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

import java.util.Map;

@ActionDescription("Aborts the jenkins builds specified by the jenkinsJobsToUse config property. Updates status for jenkins build urls in testing done section.")
public class AbortJenkinsBuilds extends BaseCommitWithJenkinsBuildsAction {

    public AbortJenkinsBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        askForJenkinsJobKeysIfBlank();
        jenkins.checkStatusOfBuilds(draft);
        log.info("");

        for (Job jenkinsJob: config.getJenkinsJobsConfig().jobs()) {
            abortJenkinsJob(draft, jenkinsJob);
        }
    }

    private void askForJenkinsJobKeysIfBlank() {
        if (StringUtils.isNotEmpty(jenkinsConfig.jenkinsJobsToUse)) {
            return;
        }
        log.info("No jenkins job keys parameter provided! (-j parameter)");
        Map<String, String> jenkinsJobsMappings = jenkinsConfig.jenkinsJobsMappings;
        if (jenkinsJobsMappings == null || jenkinsJobsMappings.isEmpty()) {
            jenkinsConfig.jenkinsJobsToUse = InputUtils.readValue("Jenkins job keys");
        } else {
            jenkinsConfig.jenkinsJobsToUse = InputUtils.readValueUntilNotBlank("Jenkins job keys (TAB for list)", jenkinsJobsMappings.keySet());
        }
    }

    private void abortJenkinsJob(ReviewRequestDraft draft, Job job) {
        JobBuild buildToAbort = draft.getMatchingJobBuild(job);

        if (buildToAbort == null) {
            log.debug("No build url found in testing done section for job {}", job.url);
            return;
        }

        if (buildToAbort.result != BuildResult.BUILDING) {
            return;
        }

        try {
            jenkins.abortJobBuild(buildToAbort);
        } catch (NotFoundException e) {
            log.warn("Could not find build to abort. Build might not have started yet");
            return;
        }

        log.info("Marking build url {} as ABORTED", buildToAbort.url);
        buildToAbort.result = BuildResult.ABORTED;
    }
}
