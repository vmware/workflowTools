package com.vmware.config.jenkins;

import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobParameter;
import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.vmware.jenkins.domain.JobParameter.NO_USERNAME_PARAMETER;
import static com.vmware.jenkins.domain.JobParameter.USERNAME_PARAM;

/**
 * Encapsulates handling of the jenkins jobs config value.
 */
public class JenkinsJobsConfig {

    private static final String USERNAME_VALUE = "$USERNAME";

    private static final String BRANCH_NAME = "$BRANCH_NAME";

    public static final String VAPP_JSON_VALUE = "$VAPP_JSON";

    public static final String ADD_ADDITIONAL_PARAMS_KEY = "ADD_ADDITIONAL_PARAMS";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Map<String, String> jenkinsJobsMappings = new HashMap<>();
    private final Map<String, String> additionalJobParameters;
    private String branchName;
    private String vappJsonParameter;

    private String[] jobsDisplayNames;
    private Map<String, String> presetParameters;

    public String jenkinsUrl;

    private List<Job> jobs = new ArrayList<>();

    public List<Job> jobs() {
        return jobs;
    }

    public int size() {
        return jobs.size();
    }

    public JenkinsJobsConfig(String jenkinsJobsToUse, String overwrittenJobNames, String[] jobsDisplayNames, Map<String, String> presetParameters,
                             String jenkinsUrl, Map<String, String> jenkinsJobsMappings, Map<String, String> additionalJobParameters,
                             String branchName, String vappJsonParameter) {
        this.presetParameters = new HashMap<>(presetParameters);
        this.jobsDisplayNames = jobsDisplayNames;
        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsJobsMappings = jenkinsJobsMappings;
        this.additionalJobParameters = additionalJobParameters;
        this.branchName = branchName;
        this.vappJsonParameter = vappJsonParameter;
        parseJobsText(jenkinsJobsToUse, overwrittenJobNames);
    }

    private void parseJobsText(String jenkinsJobsToUse, String overwrittenJobNamesToUse) {
        if (jenkinsJobsToUse == null) {
            return;
        }
        String[] jobs = jenkinsJobsToUse.split(",");
        String[] overwrittenJobNames = StringUtils.isNotBlank(overwrittenJobNamesToUse) ? overwrittenJobNamesToUse.split(",") : new String[0];
        int jobCounter = 0;
        for (String jobInfo : jobs) {
            String overwrittenJobName = overwrittenJobNames.length > jobCounter ? overwrittenJobNames[jobCounter] : null;
            parseJobInfo(jobInfo, overwrittenJobName, jobCounter++);
        }
    }

    private void parseJobInfo(String jobInfo, String overwrittenJobName, int jobCounter) {
        String mappedValue = jenkinsJobsMappings.get(jobInfo);
        Job job = new Job();
        if (mappedValue != null) {
            job.name = overwrittenJobName != null ? overwrittenJobName : jobInfo;
            jobInfo = mappedValue;
        } else if (!jobInfo.contains("&")) {
            log.info("Treating job text {} as jenkins job name since it didn't matching any mappings", jobInfo);
        }
        int pipeIndex = jobInfo.indexOf("|");
        if (pipeIndex != -1) {
            job.buildDisplayName = jobInfo.substring(0, pipeIndex);
            jobInfo = jobInfo.substring(pipeIndex + 1);
        } else {
            job.buildDisplayName = "Build";
        }
        if (jobsDisplayNames != null) {
            if (jobsDisplayNames.length > jobCounter) {
                job.buildDisplayName = jobsDisplayNames[jobCounter];
            }
        }
        if (jobInfo.contains("&")) {
            int ampersandIndex = jobInfo.indexOf("&");
            String jobName = overwrittenJobName != null ? overwrittenJobName : jobInfo.substring(0, ampersandIndex);
            job.name = jobName;
            String paramText = jobInfo.substring(ampersandIndex + 1);
            job.constructUrl(jenkinsUrl, jobName);
            job.parameters = parseJobParameters(jobName, paramText.split("&"), false, additionalJobParameters);
        } else {
            job.constructUrl(jenkinsUrl, jobInfo);
        }
        jobs.add(job);
    }

    private List<JobParameter> parseJobParameters(String jobName, String[] params, boolean specifiedParametersOnly, Map<String, String> additionalJobParameters) {
        List<JobParameter> parameters = new ArrayList<>();
        for (String param : params) {
            String[] paramPieces = param.split("=");
            if (paramPieces.length != 2) {
                throw new FatalException(
                        "Parameter {} for job {} should be of the format name=value", param, jobName);
            }
            String paramName = paramPieces[0];
            String paramValue = paramPieces[1];
            if (paramValue.contains(BRANCH_NAME)) {
                paramValue = replaceBranchNameVariableWithValue(paramValue);
            }
            if (paramName.equals(ADD_ADDITIONAL_PARAMS_KEY) && additionalJobParameters.containsKey(paramValue)) {
                List<JobParameter> additionalParams = parseJobParameters(jobName, additionalJobParameters.get(paramValue).split("&"), true,
                        additionalJobParameters);
                parameters.addAll(additionalParams);
            } else {
                parameters.add(new JobParameter(paramName, paramValue));
            }
        }
        expandParameterValues(jobName, parameters, specifiedParametersOnly);
        return parameters;
    }

    private String replaceBranchNameVariableWithValue(String paramValue) {
        paramValue = paramValue.replace(BRANCH_NAME, branchName);
        if (StringUtils.isEmpty(paramValue)) {
            paramValue = "noBranch";
        }
        return paramValue;
    }

    private void expandParameterValues(String jobName, List<JobParameter> parameters, boolean specifiedParametersOnly) {
        boolean setDefaultUsernameParam = !specifiedParametersOnly && !presetParameters.containsKey(USERNAME_PARAM);
        Iterator<JobParameter> paramIter = parameters.iterator();
        Set<String> usedPresetParams = new HashSet<>();

        for (String presetParamName : presetParameters.keySet()) {
            String presetParamValue = presetParameters.get(presetParamName);
            if (!presetParamValue.startsWith("$PARAM:")) {
                continue;
            }

            String paramNameToUseValueFor = presetParamValue.substring("$PARAM:".length());
            Optional<String> paramValue = parameters.stream()
                    .filter(param -> param.name.equals(paramNameToUseValueFor))
                    .map(param -> param.value).findFirst();

            paramValue.ifPresent(value -> {
                log.debug("Using value {} of parameter {} for parameter {}",
                        value, paramNameToUseValueFor, presetParamName);
                presetParameters.put(presetParamName, value);
            });

        }

        while (paramIter.hasNext()) {
            JobParameter parameter = paramIter.next();
            String paramName = parameter.name;
            String paramValue = parameter.value;
            if (paramName.equals(USERNAME_PARAM)) {
                setDefaultUsernameParam = false;
            }

            if (presetParameters.containsKey(paramName)) {
                usedPresetParams.add(paramName);
                paramValue = presetParameters.get(paramName);
                log.debug("Setting parameter {} to preset parameter {}", paramName, paramValue);
                if (paramValue.startsWith("$FILE:")) {
                    String fileName = paramValue.substring("$FILE:".length());
                    log.debug("Using {} file as source for parameter {}", fileName, paramName);
                    paramValue = FileUtils.readFileAsString(new File(fileName));
                    log.trace("File parameter {} output:\n{}", fileName, paramValue);
                }
            }

            while (paramValue.contains(USERNAME_VALUE)) {
                if (presetParameters.get(USERNAME_PARAM) == null) {
                    throw new FatalException("Cannot replace username parameter in jenkins job " + jobName);
                }
                paramValue = paramValue.replace(USERNAME_VALUE, presetParameters.get(USERNAME_PARAM));
            }

            if (paramName.equals(NO_USERNAME_PARAMETER) && Boolean.parseBoolean(paramValue)) {
                setDefaultUsernameParam = false;
                log.debug("Not setting default {} parameter for this job as {} is true", USERNAME_PARAM, NO_USERNAME_PARAMETER);
                paramIter.remove();
            } else {
                parameter.value = paramValue;
            }
        }

        if (setDefaultUsernameParam) {
            log.debug("Adding default user parameter {} with value {}", USERNAME_PARAM, presetParameters.get(USERNAME_PARAM));
            parameters.add(0, new JobParameter(USERNAME_PARAM, presetParameters.get(USERNAME_PARAM)));
        }

        if (vappJsonParameter != null && presetParameters.containsKey(vappJsonParameter) && specifiedParametersOnly) {
            usedPresetParams.add(vappJsonParameter);
            log.debug("Adding preset value {} for Vapp json parameter {}", presetParameters.get(vappJsonParameter), vappJsonParameter);
            parameters.add(new JobParameter(vappJsonParameter, presetParameters.get(vappJsonParameter)));
        }

        if (!specifiedParametersOnly) {
            addUnusedPresetParameters(parameters, usedPresetParams);
        }
    }

    @Override
    public String toString() {
        String jobText = "";
        for (Job job : jobs) {
            jobText = StringUtils.appendWithDelimiter(jobText, job.buildDisplayName + "|" + job.name, ",");
            jobText = StringUtils.appendWithDelimiter(jobText, job.parameters, System.lineSeparator());
        }
        return jobText;
    }

    private void addUnusedPresetParameters(List<JobParameter> parameters, Set<String> usedPresetParams) {
        Set<String> unusedPresetKeys = new HashSet<>(presetParameters.keySet());
        unusedPresetKeys.removeAll(usedPresetParams);
        for (String unusedKey : unusedPresetKeys) {
            log.debug("Adding unused preset parameter {}", unusedKey);
            parameters.add(new JobParameter(unusedKey, presetParameters.get(unusedKey)));
        }
    }
}
