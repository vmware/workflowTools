package com.vmware.action.jenkins;

import com.google.gson.Gson;
import com.vmware.BuildStatus;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobParameter;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.ParameterDefinition;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
        if (jenkinsConfig.overwrittenJenkinsJobs != null) {
            log.debug("Overwriting jobs with {}", jenkinsConfig.overwrittenJenkinsJobs);
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
            JobBuild updatedDetails = jenkins.getJobBuildDetails(newBuild);
            return updatedDetails.building;
        };
        ThreadUtils.waitForCallable(condition, config.waitTimeForBlockingWorkflowAction, TimeUnit.SECONDS, "Build failed to complete.");

        JobBuild updatedDetails = jenkins.getJobBuildDetails(newBuild);
        log.info("Job status {}", updatedDetails.realResult());
        return updatedDetails.realResult() == BuildStatus.SUCCESS;
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
        log.info("Invoking job {} using display name {}", jobToInvoke.name, jobToInvoke.buildDisplayName);

        Job job = jenkins.getJobDetails(jobToInvoke);
        JobParameters params = constructParametersForJob(jobToInvoke.parameters, job.getParameterDefinitions());

        int buildNumber = jenkins.getJobDetails(jobToInvoke).nextBuildNumber;

        JobBuild expectedNewBuild = new JobBuild(buildNumber, jobToInvoke.url);
        expectedNewBuild.name = jobToInvoke.buildDisplayName;

        if (job.getParameterDefinitions().isEmpty()) {
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
                String buildNumber = StringUtils.isNotBlank(buildwebConfig.sandboxBuildNumber) ? buildwebConfig.sandboxBuildNumber :
                        determineSandboxBuildNumber(buildwebConfig.buildDisplayName);
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
                String jsonData = vappData.getJsonData();
                if (StringUtils.isNotEmpty(jenkinsConfig.vappJsonUpdateFile)) {
                    Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().addDoubleAsIntMapper().build();
                    try {
                        Map vappJson = gson.fromJson(jsonData, Map.class);
                        Map updatedVappJson = gson.fromJson(new FileReader(jenkinsConfig.vappJsonUpdateFile), Map.class);
                        log.debug("Updating vapp json properties {}", updatedVappJson.keySet());
                        updateJson(vappJson, updatedVappJson);
                        String updatedJson = gson.toJson(vappJson);
                        log.trace(updatedJson);
                        paramValue = URLEncoder.encode(updatedJson, "UTF-8");
                    } catch (FileNotFoundException | UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        paramValue = URLEncoder.encode(jsonData, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            parameter.value = paramValue;
            paramsToUse.add(parameter);
        }

        return new JobParameters(paramsToUse);
    }

    private void updateJson(Map<String, Object> vappJson, Map<String, Object> updatedVappJson) {
        for (String jsonKey : new ArrayList<>(vappJson.keySet())) {
            Object existingValue = vappJson.get(jsonKey);
            if ((existingValue instanceof List || existingValue instanceof Map) && !updatedVappJson.containsKey(jsonKey)) {
                continue;
            }
            Object updatedValue = updatedVappJson.get(jsonKey);
            if (updatedValue == null) {
                vappJson.remove(jsonKey);
            } else if (existingValue instanceof List) {
                List existingList = (List) existingValue;
                List updatedList = (List) updatedValue;
                for (int i = 0; i < existingList.size(); i++) {
                    if (updatedList.size() <= i) {
                        existingList.remove(i);
                    } else if (existingList.get(i) instanceof Map) {
                        Map<String, Object> existingMap = (Map<String, Object>) existingList.get(i);
                        if (existingMap.values().stream().allMatch(value -> !value.getClass().isPrimitive())) {
                            updateJson(existingMap, (Map<String, Object>) updatedList.get(i));
                        } else {
                            vappJson.put(jsonKey, updatedList.get(i));
                        }
                    }
                }
            } else {
                vappJson.put(jsonKey, updatedValue);
            }
        }
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
