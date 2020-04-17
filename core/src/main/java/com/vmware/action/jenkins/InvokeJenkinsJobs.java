package com.vmware.action.jenkins;

import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.jenkins.Job;
import com.vmware.config.jenkins.JobParameter;
import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.jenkins.domain.JobBuildDetails;
import com.vmware.jenkins.domain.JobDetails;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.ParameterDefinition;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@ActionDescription("Invokes the jenkins jobs specified by the jenkinsJobsToUse config property. Adds or replaces jenkins job urls to testing done section.")
public class InvokeJenkinsJobs extends BaseCommitWithJenkinsBuildsAction {

    private static final String ASK_FOR_PARAM = "$ASK";
    private static final String SANDBOX_BUILD_NUMBER = "$SANDBOX_BUILD";

    public InvokeJenkinsJobs(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        // always run
    }

    @Override
    public void process() {
        askForJenkinsJobKeysIfBlank();

        if (jenkinsConfig.jobsDisplayNames != null) {
            log.debug("Using job display names {}", Arrays.toString(jenkinsConfig.jobsDisplayNames));
        }
        int counter = 0;
        JenkinsJobsConfig jenkinsJobsConfig = config.getJenkinsJobsConfig();
        for (Job job : jenkinsJobsConfig.jobs()) {
            if (counter == 0) {
                log.info("");
            }
            JobBuild newBuild = invokeJenkinsJob(draft, job);
            boolean success = waitForBuildToCompleteIfNecessary(newBuild);
            if (!success && counter < jenkinsJobsConfig.size() - 1 && !jenkinsConfig.ignoreJenkinsJobFailure) {
                log.warn("Build did not complete successfully, aborting running of builds");
                break;
            }
            counter++;
        }
    }

    private boolean waitForBuildToCompleteIfNecessary(final JobBuild newBuild) {
        if (!config.waitForBlockingWorkflowAction) {
            return true;
        }

        log.info("Waiting for build to complete");
        Callable<Boolean> condition = () -> {
            JobBuildDetails updatedDetails = jenkins.getJobBuildDetails(newBuild);
            return updatedDetails.building;
        };
        ThreadUtils.sleepUntilCallableReturnsTrue(condition, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS);

        JobBuildDetails updatedDetails = jenkins.getJobBuildDetails(newBuild);
        log.info("Job status {}", updatedDetails.realResult());
        return updatedDetails.realResult() == BuildResult.SUCCESS;
    }

    private void askForJenkinsJobKeysIfBlank() {
        if (StringUtils.isNotEmpty(jenkinsConfig.jenkinsJobsToUse)) {
            return;
        }
        log.info("No jenkins job keys parameter provided! (-j parameter)");
        Map<String, String> jenkinsJobsMappings = jenkinsConfig.jenkinsJobsMappings;
        if (jenkinsJobsMappings == null || jenkinsJobsMappings.isEmpty()) {
            jenkinsConfig.jenkinsJobsToUse = InputUtils.readValue("Jenkins jobs");
        } else {
            jenkinsConfig.jenkinsJobsToUse = InputUtils.readValueUntilNotBlank("Jenkins job keys (TAB for list)", jenkinsJobsMappings.keySet());
        }
    }

    private JobBuild invokeJenkinsJob(ReviewRequestDraft draft, Job jobToInvoke) {
        log.info("Invoking job {} using display name {}", jobToInvoke.name, jobToInvoke.jobDisplayName);

        JobDetails jobDetails = jenkins.getJobDetails(jobToInvoke);
        JobParameters params = constructParametersForJob(jobToInvoke.parameters, jobDetails.getParameterDefinitions());

        int buildNumber = jenkins.getJobDetails(jobToInvoke).nextBuildNumber;

        JobBuild expectedNewBuild = new JobBuild(buildNumber, jobToInvoke.url);
        expectedNewBuild.buildDisplayName = jobToInvoke.jobDisplayName;

        if (jobDetails.getParameterDefinitions().isEmpty()) {
            log.info("Invoking job {} with no parameters", expectedNewBuild.url);
            jenkins.invokeJob(jobToInvoke);
        } else {
            log.info("Invoking job {} with parameters", expectedNewBuild.url);
            jenkins.invokeJobWithParameters(jobToInvoke, params);
        }

        draft.updateTestingDoneWithJobBuild(jobToInvoke, expectedNewBuild);
        return expectedNewBuild;
    }

    private JobParameters constructParametersForJob(List<JobParameter> parameters, List<ParameterDefinition> parameterDefinitions) {
        List<JobParameter> paramsToUse = new ArrayList<>();
        for (JobParameter parameter : parameters) {
            String paramName = parameter.name;
            String paramValue = parameter.value;

            if (getDefinitionByName(parameterDefinitions, paramName) == null) {
                if (JobParameter.USERNAME_PARAM.equals(paramName)) { // username is added by default to most jobs
                    log.debug("Not adding {} parameter for job as it is not in the parameter definitions", paramName);
                } else {
                    log.info("Not adding {} parameter for job as it is not in the parameter definitions", paramName);
                }
                continue;
            }

            if (paramValue.equals(ASK_FOR_PARAM)) {
                paramValue = InputUtils.readValueUntilNotBlank("Enter " + paramName);
            }

            if (paramValue.contains(SANDBOX_BUILD_NUMBER)) {
                String buildNumber = determineSandboxBuildNumber(buildwebConfig.buildDisplayName);
                paramValue = paramValue.replace(SANDBOX_BUILD_NUMBER, buildNumber);
            }

            log.info("Setting job param {} to {}", paramName, paramValue);

            if (paramValue.equals(JenkinsJobsConfig.VAPP_JSON_VALUE)) {
                if (vappData.noVappSelected()) {
                    throw new FatalException("$VAPP_JSON paramter used but no Vapp selected");
                }
                if (!vappData.jsonDataLoaded()) {
                    throw new FatalException("$VAPP_JSON paramter used but no Vapp Json loaded");
                }
                paramValue = vappData.getJsonData();
            }

            parameter.value = paramValue;
            paramsToUse.add(parameter);
        }

        return new JobParameters(paramsToUse);
    }

    private ParameterDefinition getDefinitionByName(List<ParameterDefinition> parameterDefinitions, String name) {
        for (ParameterDefinition parameterDefinition : parameterDefinitions) {
            if (parameterDefinition.name.equals(name)) {
                return parameterDefinition;
            }
        }
        return null;
    }

}
