package com.vmware.action.jenkins;

import com.vmware.BuildResult;
import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.JenkinsJobsConfig;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.*;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.input.InputUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;

import java.util.ArrayList;
import java.util.Iterator;
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
    public String cannotRunAction() {
        return null;
    }

    @Override
    public void process() {
        askForJenkinsJobKeysIfBlank();

        JenkinsJobsConfig jobsConfig = config.getJenkinsJobsConfig();
        int counter = 0;
        for (Job job : jobsConfig.jobs()) {
            if (counter == 0) {
                log.info("");
            }
            JobBuild newBuild = invokeJenkinsJob(draft, job);
            boolean success = waitForBuildToCompleteIfNecessary(newBuild);
            if (!success && counter < jobsConfig.size() - 1 && !config.ignoreJenkinsJobFailure) {
                log.warn("Build did not complete successfully, aborting running of builds");
                break;
            }
            counter++;
        }
    }

    private boolean waitForBuildToCompleteIfNecessary(final JobBuild newBuild) {
        if (!config.waitForJenkinsJobCompletion) {
            return true;
        }

        log.info("Waiting for build to complete");
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
        if (StringUtils.isNotBlank(config.jenkinsJobsToUse)) {
            return;
        }
        log.info("No jenkins job keys parameter provided! (-j parameter)");
        if (config.jenkinsJobsMappings == null || config.jenkinsJobsMappings.isEmpty()) {
            config.jenkinsJobsToUse = InputUtils.readValue("Jenkins jobs");
        } else {
            config.jenkinsJobsToUse = InputUtils.readValueUntilNotBlank("Jenkins job keys (TAB for list)", config.jenkinsJobsMappings.keySet());
        }
    }

    private JobBuild invokeJenkinsJob(ReviewRequestDraft draft, Job jobToInvoke) {
        log.info("Invoking job {}", jobToInvoke.name);

        JobDetails jobDetails = jenkins.getJobDetails(jobToInvoke);
        JobParameters params = constructParametersForJob(jobToInvoke.parameters, jobDetails.getParameterDefinitions());

        int buildNumber = jenkins.getJobDetails(jobToInvoke).nextBuildNumber;

        JobBuild expectedNewBuild = new JobBuild(buildNumber, jobToInvoke.url);

        log.info("Invoking job {}", expectedNewBuild.url);
        jenkins.invokeJob(jobToInvoke, params);

        draft.updateTestingDoneWithJobBuild(jobToInvoke.url, expectedNewBuild);
        return expectedNewBuild;
    }

    private JobParameters constructParametersForJob(List<JobParameter> parameters, List<ParameterDefinition> parameterDefinitions) {
        for (JobParameter parameter : parameters) {
            String paramName = parameter.name;
            String paramValue = parameter.value;

            if (paramValue.equals(ASK_FOR_PARAM)) {
                paramValue = InputUtils.readValueUntilNotBlank("Enter " + paramName);
            }

            if (paramValue.contains(SANDBOX_BUILD_NUMBER)) {
                String buildNumber = determineSandboxBuildNumber();
                paramValue = paramValue.replace(SANDBOX_BUILD_NUMBER, buildNumber);
            }

            log.info("Setting job param {} to {}", paramName, paramValue);
            parameter.value = paramValue;
        }

        for (ParameterDefinition parameterDefinition : parameterDefinitions) {
            JobParameter matchingParameter = getParameterByName(parameters, parameterDefinition.name);
            if (matchingParameter != null) {
                continue;
            }
            if (parameterDefinition.defaultParameterValue == null || StringUtils.isBlank(parameterDefinition.defaultParameterValue.value)) {
                log.warn("Parameter {} is not specified and does not have a default value, assuming it is an optional parameter", parameterDefinition.name);
                continue;
            }
            JobParameter defaultParameter = new JobParameter(parameterDefinition.name, parameterDefinition.defaultParameterValue.value);
            log.info("Setting default job param {} to {}", defaultParameter.name, defaultParameter.value);
            parameters.add(defaultParameter);
        }
        return new JobParameters(parameters.toArray(new JobParameter[parameters.size()]));
    }

    private JobParameter getParameterByName(List<JobParameter> parameters, String parameterName) {
        for (JobParameter parameter : parameters) {
            if (parameterName.equals(parameter.name)) {
                return parameter;
            }
        }
        return null;
    }

    private String determineSandboxBuildNumber() {
        JobBuild sandboxBuild = draft.getMatchingJobBuild(config.buildwebUrl);
        String buildId;
        if (sandboxBuild != null) {
            buildId = sandboxBuild.id();
            if (buildId == null) {
                throw new IllegalArgumentException("No build number found in url " + sandboxBuild.url);
            }
        } else {
            buildId = InputUtils.readValueUntilNotBlank("Sandbox build number");
        }
        return buildId;
    }


}
