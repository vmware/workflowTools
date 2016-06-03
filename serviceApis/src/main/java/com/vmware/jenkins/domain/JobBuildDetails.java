package com.vmware.jenkins.domain;

import com.vmware.BuildResult;

public class JobBuildDetails {

   public String url;

   public JobBuildDetail[] actions;

   public boolean building;

   public BuildResult result;

   public String getJobBuildCommitId() {
       for (JobBuildDetail detail : actions) {
           if (detail.lastBuiltRevision != null) {
               return detail.lastBuiltRevision.SHA1;
           }
       }
       return null;
   }

    public String getJobInitiator() {
        for (JobBuildDetail detail : actions) {
            if (detail.causes != null) {
                return detail.causes[0].userId;
            }
        }
        throw new RuntimeException("Could not get username for job starter");
    }

    public BuildResult realResult() {
        return building ? BuildResult.BUILDING : result;
    }
}
