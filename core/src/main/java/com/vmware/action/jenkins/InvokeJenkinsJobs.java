package com.vmware.action.jenkins;

import com.vmware.action.base.AbstractCommitWithBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.JobParameter;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.utils.InputUtils;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@ActionDescription("Invokes the jenkins jobs specified by the jenkinsJobKeys config property. Adds or replaces jenkins job urls to testing done section.")
public class InvokeJenkinsJobs extends AbstractCommitWithBuildsAction {
    private static final String USERNAME_PARAM = "USERNAME";

    public InvokeJenkinsJobs(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException {
        if (config.jenkinsJobs == null || config.jenkinsJobs.isEmpty()) {
            log.info("Ignoring action {} as there are no jenkins jobs configured", this.getClass().getSimpleName());
            return false;
        }

        return true;
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

        for (int i = 0; i < jenkinsJobTexts.size(); i ++) {
            String jenkinsJobText = jenkinsJobTexts.get(i);
            if (i > 0) {
                log.info("");
            }
            invokeJenkinsJob(draft, jenkinsJobText);
        }
    }

    private void askForJenkinsJobKeysIfBlank() throws IOException {
        if (StringUtils.isNotBlank(config.jenkinsJobKeys)) {
            return;
        }
        log.info("No jenkins job keys parameter provided! (-j parameter)");
        config.jenkinsJobKeys = InputUtils.readValueUntilNotBlank("Jenkins job keys (TAB for list)", config.jenkinsJobs.keySet());
    }

    private void invokeJenkinsJob(ReviewRequestDraft draft, String jenkinsJobText) throws IOException, URISyntaxException, IllegalAccessException {
        String[] jenkinsJobDetails = jenkinsJobText.split(",");
        String jenkinsJobName = jenkinsJobDetails[0];
        log.info("Invoking job " + jenkinsJobName);
        Job jobToInvoke = new Job(config.jenkinsUrl + "/job/" + jenkinsJobName + "/");

        JobParameters params = generateJobParameters(jenkinsJobDetails);

        int buildNumber = jenkins.getJobDetails(jobToInvoke).nextBuildNumber;

        JobBuild expectedNewBuild = new JobBuild(buildNumber, jobToInvoke.url);

        jenkins.invokeJob(jobToInvoke, params);

        updateTestingDone(jobToInvoke, expectedNewBuild, draft);
    }

    private JobParameters generateJobParameters(String[] jenkinsJobDetails) {
        List<JobParameter> parameters = new ArrayList<JobParameter>();
        boolean foundUsernameParam = false;
        for (int i = 1; i < jenkinsJobDetails.length; i++) {
            String jenkinsParam = jenkinsJobDetails[i];
            if (jenkinsParam.equals(USERNAME_PARAM)) {
                foundUsernameParam = true;
            }
            String[] paramPieces = jenkinsParam.split("=");
            if (paramPieces.length != 2) {
                throw new IllegalArgumentException("Jenkins param " + jenkinsParam + " should be of the format name=value");
            }
            log.info("Setting job param {} to {}", paramPieces[0], paramPieces[1]);
            parameters.add(new JobParameter(paramPieces[0], paramPieces[1]));
        }

        if (!foundUsernameParam) {
            parameters.add(0, new JobParameter(USERNAME_PARAM, config.username));
        }
        return new JobParameters(parameters.toArray(new JobParameter[parameters.size()]));
    }

    private void updateTestingDone(Job jobToInvoke, JobBuild expectedNewBuild, ReviewRequestDraft draft) {
        JobBuild existingBuild = draft.getMatchingJobBuild(jobToInvoke.url);
        if (existingBuild == null ) {
            log.info("Appending {} to testing done", expectedNewBuild.url);
            draft.jobBuilds.add(expectedNewBuild);
        } else {
            log.info("Replacing existing build url {} in testing done ", existingBuild.url);
            log.info("New build url {}", expectedNewBuild.url);
            existingBuild.url = expectedNewBuild.url;
            existingBuild.result = expectedNewBuild.result;
        }
    }
}
