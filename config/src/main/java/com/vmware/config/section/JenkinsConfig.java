package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.config.jenkins.JobParameter;
import com.vmware.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class JenkinsConfig {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @ConfigurableProperty(commandLine = "-jenkinsUrl,--jenkins-url", help = "Url for jenkins server")
    public String jenkinsUrl;

    @ConfigurableProperty(commandLine = "-jcsrf,--jenkins-uses-csrf", help = "Whether the jenkins server uses CSRF header")
    public boolean jenkinsUsesCsrf;

    @ConfigurableProperty(commandLine = "-ignoreJobFailure,--ignore-jenkins-job-failure", help = "If wait for Jenkins job result is set, then ignore job failure and run the next build")
    public boolean ignoreJenkinsJobFailure;

    @ConfigurableProperty(help = "Max number of jenkins jobs to iterate over when checking for latest status of jenkins job")
    public int maxJenkinsBuildsToCheck;

    @ConfigurableProperty(commandLine = "-j,--jenkins-jobs", help = "Sets the names and parameters for the jenkins jobs to invoke. Separate jobs by commas and parameters by ampersands")
    public String jenkinsJobsToUse;

    @ConfigurableProperty(commandLine = "--job-display-names", help = "Display names to use for the jobs invoked")
    public String[] jobsDisplayNames;

    @ConfigurableProperty(commandLine = "--disable-jenkins-login", help = "Skips trying to log into jenkins if the server is not using user login module")
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

    @ConfigurableProperty(commandLine = "--testbed-parameter-name", help = "Name of the Jenkins parameter used for testbed template")
    public String testbedParameter;

    public void addJenkinsParametersFromConfigValues(Map<String, String> configValues, boolean overwriteJenkinsParameters) {
        for (String configValue : configValues.keySet()) {
            if (!configValue.startsWith("--J")) {
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
