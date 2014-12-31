package com.vmware.action.jenkins;

import com.vmware.action.base.AbstractCommitWithBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobBuildResult;
import com.vmware.rest.exception.NotFoundException;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.input.InputUtils;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@ActionDescription("Aborts the jenkins jobs specified by the jenkinsJobKeys config property. Updates status for jenkins job urls in testing done section.")
public class AbortJenkinsJobs extends AbstractCommitWithBuildsAction {

    public AbortJenkinsJobs(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        askForJenkinsJobKeysIfBlank();

        String[] jenkinsJobKeys = config.jenkinsJobKeys.split(",");
        List<String> jenkinsJobTexts = new ArrayList<String>();
        for (String jenkinsJobKey: jenkinsJobKeys) {
            String jenkinsJobText = config.jenkinsJobs.get(jenkinsJobKey);
            if (jenkinsJobText == null) {
                throw new IllegalArgumentException("No job found for jenkins job key " + jenkinsJobKey);
            }
            jenkinsJobTexts.add(jenkinsJobText);
        }
        jenkins.checkStatusOfJenkinsJobs(draft);
        log.info("");

        for (String jenkinsJobText: jenkinsJobTexts) {
            abortJenkinsJob(draft, jenkinsJobText);
        }
    }

    private void askForJenkinsJobKeysIfBlank() throws IOException {
        if (StringUtils.isNotBlank(config.jenkinsJobKeys)) {
            return;
        }
        log.info("No jenkins job keys parameter provided! (-j parameter)");
        config.jenkinsJobKeys = InputUtils.readValueUntilNotBlank("Jenkins job keys (TAB for list)", config.jenkinsJobs.keySet());
    }

    private void abortJenkinsJob(ReviewRequestDraft draft, String jenkinsJobText) throws IOException, URISyntaxException, IllegalAccessException {
        String[] jenkinsJobDetails = jenkinsJobText.split(",");
        String jenkinsJobName = jenkinsJobDetails[0];
        String expectedUrlFormat = config.jenkinsUrl + "/job/" + jenkinsJobName + "/";
        JobBuild buildToAbort = draft.getMatchingJobBuild(expectedUrlFormat);

        if (buildToAbort == null) {
            log.debug("No build url found in testing done section for job {}", expectedUrlFormat);
            return;
        }

        if (buildToAbort.result != JobBuildResult.BUILDING) {
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
        buildToAbort.result = JobBuildResult.ABORTED;
    }
}
