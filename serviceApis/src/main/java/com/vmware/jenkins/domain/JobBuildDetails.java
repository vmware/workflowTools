package com.vmware.jenkins.domain;

import java.util.Arrays;

import com.vmware.BuildResult;
import com.vmware.util.UrlUtils;

public class JobBuildDetails {

   public String url;

   public String number;

   public JobBuildAction[] actions;

   public JobBuildArtifact[] artifacts;

   public boolean building;

   public String fullDisplayName;

   public BuildResult result;

   public String getJobBuildCommitId() {
       for (JobBuildAction action : actions) {
           if (action.lastBuiltRevision != null) {
               return action.lastBuiltRevision.SHA1;
           }
       }
       return null;
   }

    public String getJobInitiator() {
       return Arrays.stream(actions).filter(action -> action.causes != null).findFirst().map(action -> action.causes[0].userId)
               .orElseThrow(() -> new RuntimeException("Could not get username for job starter for job " + fullDisplayName));
    }

    public String getJenkinsInfoUrl() {
        return UrlUtils.addRelativePaths(url, "api/json");
    }

    public String getTestReportsUIUrl() {
        return UrlUtils.addRelativePaths(url, "testngreports");
    }

    public String getTestReportsApiUrl() {
        return UrlUtils.addRelativePaths(getTestReportsUIUrl(), "api/json?depth=3");
    }

    public String fullUrlForArtifact(String pathPattern) {
       JobBuildArtifact matchingArtifact = getArtifactForPathPattern(pathPattern);
       return UrlUtils.addRelativePaths(url, "artifact", matchingArtifact.relativePath);
    }

    public BuildResult realResult() {
        return building ? BuildResult.BUILDING : result;
    }

    public JobBuildArtifact getArtifactForPathPattern(String pathPattern) {
        return Arrays.stream(artifacts).filter(artifact -> artifact.matchesPathPattern(pathPattern)).findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find artifact for path pattern " + pathPattern + " for job " + fullDisplayName));
    }

    public int number() {
       return Integer.parseInt(number);
    }
}
