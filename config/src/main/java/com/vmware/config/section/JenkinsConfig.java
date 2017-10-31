package com.vmware.config.section;

import com.vmware.config.ConfigurableProperty;

public class JenkinsConfig {

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

}
