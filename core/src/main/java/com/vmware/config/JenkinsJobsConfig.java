package com.vmware.config;

import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.vmware.jenkins.domain.JobParameter.NO_USERNAME_PARAMETER;
import static com.vmware.jenkins.domain.JobParameter.USERNAME_PARAM;

/**
 * Encapsulates handling of the jenkins jobs config value.
 */
public class JenkinsJobsConfig {

    private static final String USERNAME_VALUE = "$USERNAME";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Map<String, String> jenkinsJobsMappings;
    private Map<String, String> presetParameters;
    private String jenkinsUrl;

    private List<Job> jobs = new ArrayList<>();

    public JenkinsJobsConfig(String jenkinsJobsToUse, Map<String, String> presetParameters, String jenkinsUrl,
                             Map<String, String> jenkinsJobsMappings) {
        this.jenkinsJobsMappings = jenkinsJobsMappings;
        this.presetParameters = presetParameters;
        this.jenkinsUrl = jenkinsUrl;
        parseJobsText(jenkinsJobsToUse);
    }

    public List<Job> jobs() {
        return jobs;
    }

    public int size() {
        return jobs.size();
    }

    private void parseJobsText(String jenkinsJobsToUse) {
        String[] jobs = jenkinsJobsToUse.split(",");
        for (String jobInfo : jobs) {
            parseJobInfo(jobInfo);
        }
    }

    private void parseJobInfo(String jobInfo) {
        String mappedValue = jenkinsJobsMappings.get(jobInfo);
        if (mappedValue != null) {
            jobInfo = mappedValue;
        } else if (!jobInfo.contains("&")) {
            log.info("Treating job text {} as jenkins job name since it didn't matching any mappings", jobInfo);
        }
        if (jobInfo.contains("&")) {
            int ampersandIndex = jobInfo.indexOf("&");
            String jobName = jobInfo.substring(0, ampersandIndex);
            String paramText = jobInfo.substring(ampersandIndex + 1);
            addJobConfig(jobName, paramText.split("&"));
        } else {
            addJobConfig(jobInfo, new String[0]);
        }
    }

    private void addJobConfig(String jobName, String[] params) {
        List<JobParameter> parameters = new ArrayList<>();
        for (String param : params) {
            String[] paramPieces = param.split("=");
            if (paramPieces.length != 2) {
                throw new IllegalArgumentException("Parameter " + param + " for job " + jobName
                        + " should be of the format name=value");
            }
            parameters.add(new JobParameter(paramPieces[0], paramPieces[1]));
        }
        expandParameterValues(parameters);
        Job job = new Job(jenkinsUrl, jobName);
        job.parameters = parameters;
        jobs.add(job);
    }

    private void expandParameterValues(List<JobParameter> parameters) {
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
            }

            while (paramValue.contains(USERNAME_VALUE)) {
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
    }

    @Override
    public String toString() {
        String jobText = "";
        for (Job job : jobs) {
            List<JobParameter> params = job.parameters;
            if (!jobText.isEmpty()) {
                jobText += ",";
            }
            jobText += job.name;
            for (JobParameter param : params) {
                jobText += "&" + param.name + "=" + param.value;
            }
        }
        return jobText;
    }
}
