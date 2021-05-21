package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.jenkins.domain.JobParameter;
import com.vmware.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class JenkinsConfig {

    public static final String CONFIG_PREFIX = "--J";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @ConfigurableProperty(help = "Url for jenkins server")
    public String jenkinsUrl;

    @ConfigurableProperty(help = "Whether the jenkins server uses CSRF header")
    public boolean jenkinsUsesCsrf;

    @ConfigurableProperty(commandLine = "-ignoreJobFailure,--ignore-jenkins-job-failure", help = "If wait for Jenkins job result is set, then ignore job failure and run the next build")
    public boolean ignoreJenkinsJobFailure;

    @ConfigurableProperty(commandLine = "--max-builds", help = "Max number of jenkins builds to iterate over when checking for latest status of jenkins job")
    public int maxJenkinsBuildsToCheck;

    @ConfigurableProperty(commandLine = "--build-failure-threshold", help = "Threshold for failing tests and config being considered a normal amount")
    public int buildFailureThreshold;

    @ConfigurableProperty(commandLine = "-j,--jenkins-jobs", help = "Sets the names and parameters for the jenkins jobs to invoke. Separate jobs by commas and parameters by ampersands")
    public String jenkinsJobsToUse;

    @ConfigurableProperty(commandLine = "--job-display-names", help = "Display names to use for the jobs invoked")
    public String[] jobsDisplayNames;

    @ConfigurableProperty(help = "Skips trying to log into jenkins if the server is not using user login module")
    public boolean disableJenkinsLogin;

    @ConfigurableProperty(help = "Map of user friendly names for jenkins jobs to select from")
    public Map<String, String> jenkinsJobsMappings = new HashMap<>();

    @ConfigurableProperty(help = "Variables to use for jenkins jobs, can set specific values re command line as well, e.g. --JVAPP_NAME=test --JUSERNAME=dbiggs")
    public Map<String, String> jenkinsJobParameters = new TreeMap<>();

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(commandLine = "--include-in-progress", help = "Display output for in progress builds")
    public boolean includeInProgressBuilds;

    @ConfigurableProperty(help = "Name for Vapp metadata json Jenkins parameter")
    public String vappJsonParameter;

    @ConfigurableProperty(commandLine = "--use-vapp-json", help = "Use json metadata from Vapp")
    public boolean useVappJsonParameter;

    @ConfigurableProperty(help = "Name of the Jenkins parameter used for testbed template")
    public String testbedParameter;

    @ConfigurableProperty(commandLine = "--job-with-artifact", help = "Jenkins job to use for artifact")
    public String jobWithArtifact;

    @ConfigurableProperty(commandLine = "--job-artifact", help = "Jenkins job output artifact")
    public String jobArtifact;

    @ConfigurableProperty(commandLine = "--jenkins-view", help = "View ")
    public String jenkinsView;

    @ConfigurableProperty(commandLine = "--job-build-number", help = "Number of jenkins build to use")
    public Integer jobBuildNumber;

    @ConfigurableProperty(commandLine = "--always-download", help = "Always select a build to download artifiact from")
    public boolean alwaysDownload;

    @ConfigurableProperty(help = "Pattern for parsing commit id from build description")
    public String commitIdInDescriptionPattern;

    @ConfigurableProperty(help = "Url for displaying comparison between two build commit ids. Needs to contain named groups first and second")
    public String commitComparisonUrl;

    public boolean hasConfiguredArtifact() {
        return hasConfiguredArtifactWithoutBuildNumber() && jobBuildNumber != null;
    }

    public boolean hasConfiguredArtifactWithoutBuildNumber() {
        return StringUtils.isNotEmpty(jobArtifact) && (StringUtils.isNotEmpty(jobWithArtifact) ||  (jobsDisplayNames != null && jobsDisplayNames.length == 1));
    }

    public void addJenkinsParametersFromConfigValues(Map<String, String> configValues, boolean overwriteJenkinsParameters) {
        for (String configValue : configValues.keySet()) {
            if (!configValue.startsWith(CONFIG_PREFIX)) {
                continue;
            }
            String parameterName = configValue.substring(3);
            String parameterValue = configValues.get(configValue);
            if (!overwriteJenkinsParameters && jenkinsJobParameters.containsKey(parameterName)) {
                log.debug("Ignoring config value {} as it is already set", configValue);
                continue;
            }
            jenkinsJobParameters.put(parameterName, parameterValue);
        }
    }

    public JenkinsJobsConfig getJenkinsJobsConfig(String username, String targetBranch) {
        jenkinsJobParameters.put(JobParameter.USERNAME_PARAM, username);
        Map<String, String> presetParams = new HashMap<>(jenkinsJobParameters);
        if (useVappJsonParameter) {
            presetParams.put(vappJsonParameter, JenkinsJobsConfig.VAPP_JSON_VALUE);
        }
        Map<String, String> jobMappings = Collections.unmodifiableMap(jenkinsJobsMappings);

        return new JenkinsJobsConfig(jenkinsJobsToUse, jobsDisplayNames, Collections.unmodifiableMap(presetParams), jenkinsUrl,
                jobMappings, targetBranch, vappJsonParameter);
    }

}
