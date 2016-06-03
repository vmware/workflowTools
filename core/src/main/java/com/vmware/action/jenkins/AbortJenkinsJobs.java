package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.JobBuild;
import com.vmware.BuildResult;
import com.vmware.http.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@ActionDescription("Aborts the jenkins jobs specified by the jenkinsJobKeys config property. Updates status for jenkins job urls in testing done section.")
public class AbortJenkinsJobs extends BaseCommitWithJenkinsBuildsAction {

    public AbortJenkinsJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        askForJenkinsJobKeysIfBlank();

        String[] jenkinsJobKeys = config.jenkinsJobKeys.split(",");
        List<String> jenkinsJobTexts = new ArrayList<String>();
        for (String jenkinsJobKey: jenkinsJobKeys) {
            String jenkinsJobText = config.getJenkinsJobValue(jenkinsJobKey);
            if (jenkinsJobText == null) {
                log.info("No job found matching text {}, treating as job value", jenkinsJobKey);
                jenkinsJobTexts.add(jenkinsJobKey);
            } else {
                jenkinsJobTexts.add(jenkinsJobText);
            }
        }
        jenkins.checkStatusOfJenkinsJobs(draft);
        log.info("");

        for (String jenkinsJobText: jenkinsJobTexts) {
            abortJenkinsJob(draft, jenkinsJobText);
        }
    }

    private void askForJenkinsJobKeysIfBlank() {
        if (StringUtils.isNotBlank(config.jenkinsJobKeys)) {
            return;
        }
        log.info("No jenkins job keys parameter provided! (-j parameter)");
        if (config.jenkinsJobs == null || config.jenkinsJobs.isEmpty()) {
            config.jenkinsJobKeys = InputUtils.readValue("Jenkins job keys");
        } else {
            config.jenkinsJobKeys = InputUtils.readValueUntilNotBlank("Jenkins job keys (TAB for list)", config.jenkinsJobs.keySet());
        }
    }

    private void abortJenkinsJob(ReviewRequestDraft draft, String jenkinsJobText) {
        String[] jenkinsJobDetails = jenkinsJobText.split("&");
        String jenkinsJobName = jenkinsJobDetails[0];
        String expectedUrlFormat = config.jenkinsUrl + "/job/" + jenkinsJobName + "/";
        JobBuild buildToAbort = draft.getMatchingJobBuild(expectedUrlFormat);

        if (buildToAbort == null) {
            log.debug("No build url found in testing done section for job {}", expectedUrlFormat);
            return;
        }

        if (buildToAbort.result != BuildResult.BUILDING) {
            return;
        }

        log.info("Aborting build {}", buildToAbort.url);
        try {
            jenkins.stopJobBuild(buildToAbort);
        } catch (NotFoundException e) {
            log.warn("Could not find build to abort. Build might not have started yet");
            return;
        }

        log.info("Marking build url {} as ABORTED", buildToAbort.url);
        buildToAbort.result = BuildResult.ABORTED;
    }
}
