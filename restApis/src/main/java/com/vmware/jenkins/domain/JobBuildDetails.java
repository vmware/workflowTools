package com.vmware.jenkins.domain;

public class JobBuildDetails {

   public JobBuildDetail[] actions;

   public boolean building;

   public JobBuildResult result;

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
}
