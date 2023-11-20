package com.vmware;

import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobParameter;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.HomePage;
import com.vmware.http.exception.NotFoundException;
import com.vmware.jenkins.domain.TestResults;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestJenkinsApi extends BaseTests {

    private String jenkinsUsername;
    private Jenkins jenkins;

    @Before
    public void init() {
        String jenkinsUrl = testProperties.getProperty("jenkins.url");
        jenkinsUsername = testProperties.getProperty("jenkins.username");
        jenkins = new Jenkins(jenkinsUrl, jenkinsUsername, true, true, null);
    }

    @Test
    public void canGetJobsListing() {
        HomePage jobs = jenkins.getHomePage();
        assertNotNull(jobs);
    }

    @Test
    public void canGetPrecommitUnitTestsJob() {
        HomePage jobs = jenkins.getHomePage();
        Job precommitUnitJob = jobs.getPrecommitUnitTestsJob();
        assertNotNull(precommitUnitJob);
    }

    @Test(expected = NotFoundException.class)
    public void cannotInvokeJunkBuild() {
        Job precommitJob = jenkins.getHomePage().getPrecommitUnitTestsPostgresJob();

        precommitJob.url = precommitJob.url.replace("postgres", "postgres1");
        jenkins.invokeJobWithParameters(precommitJob, new JobParameters(Collections.<JobParameter>emptyList()));
    }

    @Test
    public void canInvokePrecommitUnitTestsJob() {
        Job precommitJob = jenkins.getHomePage().getPrecommitUnitTestsPostgresJob();

        Job job = jenkins.getJobDetails(precommitJob);
        JobParameter usernameParam = new JobParameter("USERNAME", jenkinsUsername);
        jenkins.invokeJobWithParameters(precommitJob, new JobParameters(Collections.singletonList(usernameParam)));

        JobBuild jobBuild = jenkins.getJobBuildDetails(job.lastBuild);
        assertNotNull(jobBuild.getJobBuildCommitId());
    }

    @Test
    public void canParseApiToken() {
        String sampleText = "<input id=\"apiToken\" name=\"_.apiToken\" value=\"a2b4a16b93169028a466451b82a6bec1\"";
        Matcher tokenMatcher = Pattern.compile("name=\"_\\.apiToken\"\\s+value=\"(\\w+)\"").matcher(sampleText);
        assertTrue(tokenMatcher.find());
        assertEquals("a2b4a16b93169028a466451b82a6bec1", tokenMatcher.group(1));
    }

    @Test
    public void getTests() {
        JobBuild build = jenkins.getJobBuildDetails("CToT4-Networking-Legacy-sp-main-postgres-Continuous", 2254);
        TestResults results = jenkins.getJobBuildTestResultsViaTestNGResultFiles(build);
        assertNotNull(results);
    }

}
