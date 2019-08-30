package com.vmware.config.jenkins;

import com.vmware.util.FileUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.scm.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.vmware.config.jenkins.JobParameter.NO_USERNAME_PARAMETER;
import static com.vmware.config.jenkins.JobParameter.USERNAME_PARAM;

/**
 * Encapsulates handling of the jenkins jobs config value.
 */
public class JenkinsJobsConfig {

    private static final String USERNAME_VALUE = "$USERNAME";

    private static final String BRANCH_NAME = "$BRANCH_NAME";

    public static final String VAPP_JSON_VALUE = "$VAPP_JSON";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Map<String, String> jenkinsJobsMappings = new HashMap<>();
    private String branchName;
    private String vappJsonParameter;

    private String[] jobsDisplayNames;
    private Map<String, String> presetParameters = new TreeMap<>();

    public String jenkinsUrl;

    private List<Job> jobs = new ArrayList<>();

    public List<Job> jobs() {
        return jobs;
    }

    public int size() {
        return jobs.size();
    }

    public JenkinsJobsConfig(String jenkinsJobsToUse, String[] jobsDisplayNames, Map<String, String> presetParameters,
                             String jenkinsUrl, Map<String, String> jenkinsJobsMappings, String branchName, String vappJsonParameter) {
        this.presetParameters = presetParameters;
        this.jobsDisplayNames = jobsDisplayNames;
        this.jenkinsUrl = jenkinsUrl;
        this.jenkinsJobsMappings = jenkinsJobsMappings;
        this.branchName = branchName;
        this.vappJsonParameter = vappJsonParameter;
        parseJobsText(jenkinsJobsToUse);
    }

    private void parseJobsText(String jenkinsJobsToUse) {
        if (jenkinsJobsToUse == null) {
            return;
        }
        String[] jobs = jenkinsJobsToUse.split(",");
        int jobCounter = 0;
        for (String jobInfo : jobs) {
            parseJobInfo(jobInfo, jobCounter++);
        }
    }

    private void parseJobInfo(String jobInfo, int jobCounter) {
        String mappedValue = jenkinsJobsMappings.get(jobInfo);
        Job job = new Job();
        if (mappedValue != null) {
            job.jobDisplayName = jobInfo;
            jobInfo = mappedValue;
        } else if (!jobInfo.contains("&")) {
            log.info("Treating job text {} as jenkins job name since it didn't matching any mappings", jobInfo);
        }
        int pipeIndex = jobInfo.indexOf("|");
        if (pipeIndex != -1) {
            job.jobDisplayName = jobInfo.substring(0, pipeIndex);
            jobInfo = jobInfo.substring(pipeIndex + 1);
        } else {
            job.jobDisplayName = "Build";
        }
        if (jobsDisplayNames != null) {
            if (jobsDisplayNames.length > jobCounter) {
                job.jobDisplayName = jobsDisplayNames[jobCounter];
            }
        }
        if (jobInfo.contains("&")) {
            int ampersandIndex = jobInfo.indexOf("&");
            String jobName = jobInfo.substring(0, ampersandIndex);
            job.name = jobName;
            String paramText = jobInfo.substring(ampersandIndex + 1);
            job.constructUrl(jenkinsUrl, jobName);
            job.parameters = parseJobParameters(jobName, paramText.split("&"));
        } else {
            job.constructUrl(jenkinsUrl, jobInfo);
        }
        jobs.add(job);
    }

    private List<JobParameter> parseJobParameters(String jobName, String[] params) {
        List<JobParameter> parameters = new ArrayList<>();
        for (String param : params) {
            String[] paramPieces = param.split("=");
            if (paramPieces.length != 2) {
                throw new FatalException(
                        "Parameter {} for job {} should be of the format name=value", param, jobName);
            }
            String paramValue = paramPieces[1];
            if (paramValue.contains(BRANCH_NAME)) {
                paramValue = replaceBranchNameVariableWithValue(paramValue);
            }
            parameters.add(new JobParameter(paramPieces[0], paramValue));
        }
        expandParameterValues(jobName, parameters);
        return parameters;
    }

    private String replaceBranchNameVariableWithValue(String paramValue) {
        paramValue = paramValue.replace(BRANCH_NAME, branchName);
        if (StringUtils.isBlank(paramValue)) {
            paramValue = "noBranch";
        }
        return paramValue;
    }

    private void expandParameterValues(String jobName, List<JobParameter> parameters) {
        boolean setDefaultUsernameParam = true;
        Iterator<JobParameter> paramIter = parameters.iterator();
        while (paramIter.hasNext()) {
            JobParameter parameter = paramIter.next();
            String paramName = parameter.name;
            String paramValue = parameter.value;
            if (paramName.equals(USERNAME_PARAM)) {
                setDefaultUsernameParam = false;
            }

            if (presetParameters.containsKey(paramName)) {
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

            if (paramName.equals(NO_USERNAME_PARAMETER) && Boolean.valueOf(paramValue)) {
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

        if (vappJsonParameter != null && presetParameters.containsKey(vappJsonParameter)) {
            log.debug("Adding Vapp json parameter {}", vappJsonParameter, presetParameters.get(vappJsonParameter));
            parameters.add(new JobParameter(vappJsonParameter, presetParameters.get(vappJsonParameter)));
        }
    }

    @Override
    public String toString() {
        String jobText = "";
        for (Job job : jobs) {
            jobText = StringUtils.appendWithDelimiter(jobText, job.jobDisplayName + "|" + job.name, ",");
            jobText = StringUtils.appendWithDelimiter(jobText, job.parameters, "&");
        }
        return jobText;
    }
}
