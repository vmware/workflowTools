package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class JobView extends BaseDbClass {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String name;

    public String url;

    @Expose(serialize = false, deserialize = false)
    public int lastFetchAmount;

    @DbSaveIgnore
    public Job[] jobs;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    private DbUtils dbUtils;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    private Map<Job, List<TestResult>> failedTests = new HashMap<>();

    public void setDbUtils(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    public void populateFromDb() {
        if (dbUtils == null) {
            return;
        }

        JobView savedView = dbUtils.queryUnique(JobView.class, "SELECT * FROM JOB_VIEW WHERE URL = ?", url);
        if (savedView != null) {
            log.info("Last fetched amount was {}", savedView.lastFetchAmount);
            id = savedView.id;
            lastFetchAmount = savedView.lastFetchAmount;
        }
    }

    public void addFailingTests(Job job, List<TestResult> results) {
        failedTests.put(job, results);
    }

    public Map<Job, List<TestResult>> getFailedTests() {
        return failedTests;
    }


    public void populateJobsFromDb() {
        this.jobs = dbUtils.query(Job.class,"SELECT * FROM JOB where VIEW_ID = ?", id).toArray(new Job[0]);
        Arrays.stream(jobs).forEach(job -> job.setDbUtils(dbUtils));

        Arrays.stream(jobs).forEach(job -> {
            job.loadTestResultsFromDb();
            List<TestResult> failedTests = job.createFailingTestsList(lastFetchAmount);
            if (!failedTests.isEmpty()) {
                this.addFailingTests(job, failedTests);
            }
        });
    }

    public void updateInDb() {
        if (dbUtils != null) {
            dbUtils.update(this);
        }
    }

    public List<Job> usableJobs(int maxJenkinsBuildsToCheck) {
        List<Job> usableJobs = Arrays.stream(jobs).filter(job -> {
            if (job.lastCompletedBuild == null) {
                log.info("Skipping {} as there are no recent completed builds", job.name);
                return false;
            }
            if (job.lastUnstableBuild == null) {
                log.info("Skipping {} as there are no recent unstable builds", job.name);
                return false;
            }

            if (job.lastUnstableBuildAge() > maxJenkinsBuildsToCheck) {
                log.info("Skipping {} as last unstable build was {} builds ago", job.name, job.lastUnstableBuildAge());
                return false;
            }
            job.setDbUtils(dbUtils);
            return true;
        }).collect(toList());

        if (dbUtils == null) {
            return usableJobs;
        }

        if (id == null) {
            dbUtils.insertIfNeeded(this, "SELECT * FROM JOB_VIEW WHERE NAME = ?", name);
        }
        usableJobs.forEach(job -> {
            job.viewId = id;
            dbUtils.insertIfNeeded(job, "SELECT * FROM JOB WHERE URL = ?", job.url);
        });
        return usableJobs;
    }

    public long failingTestCount() {
        if (!failedTests.isEmpty()) {
            return failedTests.values().stream().mapToInt(List::size).sum();
        } else {
            return Arrays.stream(jobs).mapToLong(job -> job.failingTestCount(lastFetchAmount)).sum();
        }
    }
}
