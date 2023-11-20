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

    @ConfigurableProperty(commandLine = "--max-builds-to-keep", help = "Max number of jenkins builds to keep in test results database, cannot be smaller than the maxJenkinsBuildsToCheck")
    public int maxJenkinsBuildsToKeep;

    @ConfigurableProperty(commandLine = "--number-of-failures", help = "Number of failures needed to consider a test consistently failing")
    public int numberOfFailuresNeededToBeConsistentlyFailing;

    @ConfigurableProperty(commandLine = "-j,--jenkins-jobs", help = "Sets the names and parameters for the jenkins jobs to invoke. Separate jobs by commas and parameters by ampersands")
    public String jenkinsJobsToUse;

    @ConfigurableProperty(commandLine = "--overwritten-jenkins-jobs", help = "Overwrites the jobs to use")
    public String overwrittenJenkinsJobs;

    @ConfigurableProperty(commandLine = "--job-display-names", help = "Display names to use for the jobs invoked")
    public String[] jobsDisplayNames;

    @ConfigurableProperty(commandLine = "--disable-login", help = "Skips trying to log into jenkins if the server is not using user login module")
    public boolean disableJenkinsLogin;

    @ConfigurableProperty(help = "Map of user friendly names for jenkins jobs to select from")
    public Map<String, String> jenkinsJobsMappings = new HashMap<>();

    @ConfigurableProperty(help = "Variables to use for jenkins jobs, can set specific values re command line as well, e.g. --JVAPP_NAME=test --JUSERNAME=dbiggs")
    public Map<String, String> jenkinsJobParameters = new TreeMap<>();

    @ConfigurableProperty(help = "Used to add additional parameters to a job by matching against a config parameter. E.g. the key would be the username|usernameValue: properties")
    public Map<String, String> jenkinsJobsAdditionalParameters = new TreeMap<>();

    @ConfigurableProperty(help = "Override default behavior of using testng results by specifying an artifact name tha identifies a different type of job and a relative url for the tests")
    public Map<String, String> testReportsUrlOverrides = new HashMap<>();

    @ConfigurableProperty(commandLine = "--log-line-count", help = "How many lines of the log to show")
    public int logLineCount;

    @ConfigurableProperty(commandLine = "--include-in-progress", help = "Display output for in progress builds")
    public boolean includeInProgressBuilds;

    @ConfigurableProperty(commandLine = "--vapp-json-parameter-name", help = "Name for Vapp metadata json Jenkins parameter")
    public String vappJsonParameter;

    @ConfigurableProperty(commandLine = "--use-vapp-json", help = "Use json metadata from Vapp")
    public boolean useVappJsonParameter;

    @ConfigurableProperty(commandLine = "--vapp-json-update-file", help = "File to update vapp json with")
    public String vappJsonUpdateFile;

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

    @ConfigurableProperty(commandLine = "--create-test-failures-database", help = "Will run test failures database creation script")
    public boolean createTestFailuresDatabase;

    @ConfigurableProperty(help = "Group Jobs by name patterns")
    public String[] groupByNamePatterns;

    @ConfigurableProperty(commandLine = "--regenerate-html", help = "Regenerate failures from database only")
    public boolean regenerateHtml;

    @ConfigurableProperty(commandLine = "--refetch-count", help = "Refetch test results for latest number of builds from Jenkins. E.g. a value of 1 means refetch the latest build per job")
    public int refetchCount;

    @ConfigurableProperty(commandLine = "--test-name", help = "Test name to search for in test database")
    public String testName;

    @ConfigurableProperty(help = "Url to use to search by test method name")
    public String testMethodNameSearchUrl;

    @ConfigurableProperty(help = "Regex pattern to parse log id from exception message")
    public String testIdPattern;

    @ConfigurableProperty(help = "Url template to use for creating a log url using a logging id")
    public String testLogIdUrlTemplate;

    @ConfigurableProperty(help = "Url template to use for creating a log url using the test name")
    public String testNameLogUrlTemplate;

    @ConfigurableProperty(help = "Number of days after which to show job date")
    public int daysOldForShowingJobDate;

    @ConfigurableProperty(help = "Regex pattern to match before config methods if they ran at the same time as say skipped methods")
    public String beforeConfigMethodPattern;;

    @ConfigurableProperty(help = "Regex pattern to match after config methods if they ran at the same time as say skipped methods")
    public String afterConfigMethodPattern;

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

    public JenkinsJobsConfig getJenkinsJobsConfig(String username, String targetBranch, Map<String, String> relevantAdditionalParameters) {
        jenkinsJobParameters.put(JobParameter.USERNAME_PARAM, username);
        Map<String, String> presetParams = new HashMap<>(jenkinsJobParameters);
        if (useVappJsonParameter) {
            presetParams.put(vappJsonParameter, JenkinsJobsConfig.VAPP_JSON_VALUE);
        }
        Map<String, String> jobMappings = Collections.unmodifiableMap(jenkinsJobsMappings);


        return new JenkinsJobsConfig(jenkinsJobsToUse, overwrittenJenkinsJobs, jobsDisplayNames, Collections.unmodifiableMap(presetParams), jenkinsUrl,
                jobMappings, relevantAdditionalParameters, targetBranch, vappJsonParameter);
    }

}
