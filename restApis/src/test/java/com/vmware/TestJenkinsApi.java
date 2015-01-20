package com.vmware;

import com.vmware.jenkins.Jenkins;
import com.vmware.jenkins.domain.Job;
import com.vmware.jenkins.domain.JobBuildDetails;
import com.vmware.jenkins.domain.JobDetails;
import com.vmware.jenkins.domain.JobParameter;
import com.vmware.jenkins.domain.JobParameters;
import com.vmware.jenkins.domain.JobsList;
import com.vmware.rest.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestJenkinsApi extends AbstractTestRestApi{

    private String jenkinsUsername;
    private Jenkins jenkins;

    @Before
    public void init() throws IOException, URISyntaxException, IllegalAccessException {
        String jenkinsUrl = testProperties.getProperty("jenkins.url");
        jenkinsUsername = testProperties.getProperty("jenkins.username");
        jenkins = new Jenkins(jenkinsUrl, jenkinsUsername, true, false);
        jenkins.setupAuthenticatedConnection();
    }

    @Test
    public void canGetJobsListing() throws IOException, URISyntaxException, IllegalAccessException {
        JobsList jobs = jenkins.getJobsListing();
        assertNotNull(jobs);
    }

    @Test
    public void canGetPrecommitUnitTestsJob() throws IOException, URISyntaxException, IllegalAccessException {
        JobsList jobs = jenkins.getJobsListing();
        Job precommitUnitJob = jobs.getPrecommitUnitTestsJob();
        assertNotNull(precommitUnitJob);
    }

    @Test(expected = NotFoundException.class)
    public void cannotInvokeJunkBuild() throws IOException, URISyntaxException, IllegalAccessException {
        Job precommitJob = jenkins.getJobsListing().getPrecommitUnitTestsPostgresJob();

        precommitJob.url = precommitJob.url.replace("postgres", "postgres1");
        jenkins.invokeJob(precommitJob, new JobParameters(new JobParameter[0]));
    }

    @Test
    public void canInvokePrecommitUnitTestsJob() throws IOException, URISyntaxException, IllegalAccessException {
        Job precommitJob = jenkins.getJobsListing().getPrecommitUnitTestsPostgresJob();

        JobDetails jobDetails = jenkins.getJobDetails(precommitJob);
        JobParameter usernameParam = new JobParameter("USERNAME", jenkinsUsername);
        jenkins.invokeJob(precommitJob, new JobParameters(new JobParameter[] {usernameParam}));

        JobBuildDetails jobBuildDetails = jenkins.getJobBuildDetails(jobDetails.lastBuild);
        assertNotNull(jobBuildDetails.getJobBuildCommitId());

    }

    @Test
    public void canParseApiToken() {
        String sampleText = "<input id=\"apiToken\" name=\"_.apiToken\" value=\"a2b4a16b93169028a466451b82a6bec1\"";
        Matcher tokenMatcher = Pattern.compile("name=\"_\\.apiToken\"\\s+value=\"(\\w+)\"").matcher(sampleText);
        assertTrue(tokenMatcher.find());
        assertEquals("a2b4a16b93169028a466451b82a6bec1", tokenMatcher.group(1));
    }

}
