package com.vmware.jenkins.domain;

public class JobsList {

    public Job[] jobs;

    public Job getPrecommitUnitTestsJob() {
        return getJobByName("horizon-workspace-service-pre-commit-unit-tests-all");
    }

    public Job getPrecommitUnitTestsPostgresJob() {
        return getJobByName("horizon-workspace-service-pre-commit-unit-tests-postgres");
    }


    public Job getJobByName(String jobName) {
        for (Job job : jobs) {
            if (job.name.equals(jobName)) {
                return job;
            }
        }
        throw new RuntimeException("No job found for name " + jobName);
    }


}
