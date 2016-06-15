package com.vmware.action.jenkins;

import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.*;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@ActionDescription("Invokes the jenkins jobs specified by the jenkinsJobKeys config property. Adds or replaces jenkins job urls to testing done section.")
public class InvokeJenkinsJobs extends BaseCommitWithJenkinsBuildsAction {
    private static final String USERNAME_PARAM = "USERNAME";
    private static final String NO_USERNAME = "NONE";
    private static final String ASK_FOR_PARAM = "$ASK";
    private static final String SANDBOX_BUILD_NUMBER = "$SANDBOX_BUILD";
    private static final String USERNAME_VALUE = "$USERNAME";

    public InvokeJenkinsJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        return null;
    }

    @Override
    public void process() {
        askForJenkinsJobKeysIfBlank();

        String[] jenkinsJobKeys = config.jenkinsJobKeys.split(",");
        List<String> jenkinsJobTexts = new ArrayList<String>();
        for (String jenkinsJobKey: jenkinsJobKeys) {
            String jenkinsJobText = config.getJenkinsJobValue(jenkinsJobKey);
            if (jenkinsJobText == null) {
                log.info("Treating {} as job value", jenkinsJobKey);
                jenkinsJobTexts.add(jenkinsJobKey);
            } else {
                jenkinsJobTexts.add(jenkinsJobText);
            }
        }

        for (int i = 0; i < jenkinsJobTexts.size(); i ++) {
            String jenkinsJobText = jenkinsJobTexts.get(i);
            if (i > 0) {
                log.info("");
            }
            JobBuild newBuild = invokeJenkinsJob(draft, jenkinsJobText);
            boolean success = waitForJobToCompleteIfNecessary(newBuild);
            if (!success && i < jenkinsJobTexts.size() - 1 && !config.ignoreJenkinsJobFailure) {
                log.warn("Job did not complete successfully, aborting running of jobs");
                break;
            }
        }
    }

    private boolean waitForJobToCompleteIfNecessary(final JobBuild newBuild) {
        if (!config.waitForJenkinsJobCompletion) {
            return true;
        }

        log.info("Waiting for job to complete");
        Callable<Boolean> condition = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                JobBuildDetails updatedDetails = jenkins.getJobBuildDetails(newBuild);
                return updatedDetails.building;
            }
        };
        ThreadUtils.sleepUntilCallableReturnsTrue(condition, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);

        JobBuildDetails updatedDetails = jenkins.getJobBuildDetails(newBuild);
        log.info("Job status {}", updatedDetails.realResult());
        return updatedDetails.realResult() == BuildResult.SUCCESS;
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

    private JobBuild invokeJenkinsJob(ReviewRequestDraft draft, String jenkinsJobText) {
        String[] jenkinsJobDetails = jenkinsJobText.split("&");
        String jenkinsJobName = jenkinsJobDetails[0];
        log.info("Invoking job " + jenkinsJobName);
        Job jobToInvoke = new Job(config.jenkinsUrl + "/job/" + jenkinsJobName + "/");

        JobParameters params = generateJobParameters(jenkinsJobDetails);

        int buildNumber = jenkins.getJobDetails(jobToInvoke).nextBuildNumber;

        JobBuild expectedNewBuild = new JobBuild(buildNumber, jobToInvoke.url);

        log.info("Invoking job {}", expectedNewBuild.url);
        jenkins.invokeJob(jobToInvoke, params);

        draft.updateTestingDoneWithJobBuild(jobToInvoke.url, expectedNewBuild);
        return expectedNewBuild;
    }

    private JobParameters generateJobParameters(String[] jenkinsJobDetails) {
        List<JobParameter> parameters = new ArrayList<JobParameter>();
        boolean foundUsernameParam = false;
        for (int i = 1; i < jenkinsJobDetails.length; i++) {
            String jenkinsParam = jenkinsJobDetails[i];
            String[] paramPieces = jenkinsParam.split("=");
            if (paramPieces.length != 2) {
                throw new IllegalArgumentException("Jenkins param " + jenkinsParam + " should be of the format name=value");
            }
            String paramName = paramPieces[0];
            String paramValue = paramPieces[1];
            if (paramName.equals(USERNAME_PARAM)) {
                foundUsernameParam = true;
            }

            if (paramValue.equals(ASK_FOR_PARAM)) {
                paramValue = InputUtils.readValueUntilNotBlank("Enter " + paramName);
            }

            if (paramValue.contains(USERNAME_VALUE)) {
                paramValue = paramValue.replace(USERNAME_VALUE, config.username);
            }

            if (paramValue.contains(SANDBOX_BUILD_NUMBER)) {
                JobBuild sandboxBuild = draft.getMatchingJobBuild(config.buildwebApiUrl);
                if (sandboxBuild == null) {
                    throw new IllegalArgumentException("No sandbox build found in commit!");
                }
                String buildId = sandboxBuild.id();
                if (buildId == null) {
                    throw new IllegalArgumentException("No build number found in url " + sandboxBuild.url);
                }
                paramValue = paramValue.replace(SANDBOX_BUILD_NUMBER, buildId);
            }

            if (paramName.equals(USERNAME_PARAM) && paramValue.equals(NO_USERNAME)) {
                log.info("Ignoring {} parameter for this job", USERNAME_PARAM);
            } else {
                log.info("Setting job param {} to {}", paramName, paramValue);
                parameters.add(new JobParameter(paramName, paramValue));
            }
        }

        if (!foundUsernameParam) {
            parameters.add(0, new JobParameter(USERNAME_PARAM, config.username));
        }
        return new JobParameters(parameters.toArray(new JobParameter[parameters.size()]));
    }


}
