package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;
import com.vmware.config.jenkins.JenkinsJobsConfig;
import com.vmware.config.jenkins.JobParameter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class JenkinsConfig {

    @ConfigurableProperty(commandLine = "-u,--username", help = "Username to use for jenkins, jira and review board")
    public String username;

    @ConfigurableProperty(commandLine = "-jenkinsUrl,--jenkins-url", help = "Url for jenkins server")
    public String jenkinsUrl;

    @ConfigurableProperty(commandLine = "-jcsrf,--jenkins-uses-csrf", help = "Whether the jenkins server uses CSRF header")
    public boolean jenkinsUsesCsrf;

    @ConfigurableProperty(commandLine = "-waitForJenkins,--wait-for-jenkins", help = "Waits for jenkins job to complete, when running multiple jobs, waits for previous one to complete before starting next one")
    public boolean waitForJenkinsJobCompletion;

    @ConfigurableProperty(commandLine = "-ignoreJobFailure,--ignore-jenkins-job-failure", help = "If wait for Jenkins job result is set, then ignore job failure and run the next build")
    public boolean ignoreJenkinsJobFailure;

    @ConfigurableProperty(help = "Max number of jenkins jobs to iterate over when checking for latest status of jenkins job")
    public int maxJenkinsBuildsToCheck;

    @ConfigurableProperty(commandLine = "-j,--jenkins-jobs", help = "Sets the names and parameters for the jenkins jobs to invoke. Separate jobs by commas and parameters by ampersands")
    public String jenkinsJobsToUse;

    @ConfigurableProperty(commandLine = "--disable-jenkins-login", help = "Skips trying to log into jenkins if the server is not using user login module")
    public boolean disableJenkinsLogin;

    @ConfigurableProperty(help = "Map of user friendly names for jenkins jobs to select from")
    public Map<String, String> jenkinsJobsMappings = new HashMap<>();

    @ConfigurableProperty(help = "Variables to use for jenkins jobs, can set specific values re command line as well, e.g. --JVAPP_NAME=test --JUSERNAME=dbiggs")
    public Map<String, String> jenkinsJobParameters = new TreeMap<>();

    public JenkinsJobsConfig getJenkinsJobsConfig() {
        jenkinsJobParameters.put(JobParameter.USERNAME_PARAM, username);
        Map<String, String> presetParams = Collections.unmodifiableMap(jenkinsJobParameters);
        Map<String, String> jobMappings = Collections.unmodifiableMap(jenkinsJobsMappings);

        return new JenkinsJobsConfig(jenkinsJobsToUse, presetParams, jenkinsUrl, jobMappings);
    }

}
