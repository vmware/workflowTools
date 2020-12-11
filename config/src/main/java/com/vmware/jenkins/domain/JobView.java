package com.vmware.jenkins.domain;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

public class JobView extends BaseDbClass {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String name;

    public String url;

    @Expose(serialize = false, deserialize = false)
    public int lastFetchAmount;

    @DbSaveIgnore
    public Job[] jobs;

    public void populateFromDb(DbUtils dbUtils) {
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

    public List<Job> usableJobs(DbUtils dbUtils, int maxJenkinsBuildsToCheck) {
        List<Job> usableJobs = Arrays.stream(jobs).filter(jobDetails -> {
            if (jobDetails.lastCompletedBuild == null) {
                log.info("Skipping {} as there are no recent completed builds", jobDetails.name);
                return false;
            }
            if (jobDetails.lastBuildWasSuccessful()) {
                log.info("Skipping {} as most recent build {} was successful", jobDetails.name, jobDetails.lastStableBuild.buildNumber);
                return false;
            }
            if (jobDetails.lastUnstableBuild == null) {
                log.info("Skipping {} as there are no recent unstable builds", jobDetails.name);
                return false;
            }

            if (jobDetails.lastUnstableBuildAge() > maxJenkinsBuildsToCheck) {
                log.info("Skipping {} as last unstable build was {} builds ago", jobDetails.name, jobDetails.lastUnstableBuildAge());
                return false;
            }
            return true;
        }).collect(toList());

        if (dbUtils == null) {
            return usableJobs;
        }

        try (Connection connection = dbUtils.createConnection()) {
            if (id == null) {
                dbUtils.insertIfNeeded(connection, this, "SELECT * FROM JOB_VIEW WHERE NAME = ?", name);
            }
            usableJobs.forEach(job -> {
                job.viewId = id;
                dbUtils.insertIfNeeded(connection, job, "SELECT * FROM JOB WHERE URL = ?", job.url);
            });
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
        return usableJobs;
    }
}
