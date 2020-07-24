package com.vmware.action.jenkins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuildArtifact;
import com.vmware.jenkins.domain.JobBuildDetails;
import com.vmware.util.FileUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Download a specified artifact from a jenkins build.")
public class DownloadBuildArtifact extends BaseCommitWithJenkinsBuildsAction {
    public DownloadBuildArtifact(WorkflowConfig config) {
        super(config, true);
        super.addFailWorkflowIfBlankProperties("jobArtifact", "destinationFile");
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (!jenkinsConfig.hasConfiguredArtifact() && draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl).isEmpty()) {
            exitDueToFailureCheck("Jenkins artifact is not configured and there are no builds in the commit testing done section");
        }
    }

    @Override
    public void process() {
        JobBuildDetails buildDetails = getJobBuildDetails();
        replacementVariables.addVariable(ReplacementVariables.VariableName.BUILD_NUMBER, buildDetails.number);

        String fullUrl = buildDetails.fullUrlForArtifact(jenkinsConfig.jobArtifact);
        String destinationFilePath = replacementVariables.replaceVariablesInValue(fileSystemConfig.destinationFile);
        if (new File(destinationFilePath).isDirectory()) {
            JobBuildArtifact matchingArtifact = buildDetails.getArtifactForPathPattern(jenkinsConfig.jobArtifact);
            destinationFilePath = UrlUtils.addRelativePaths(destinationFilePath, FileUtils.appendToFileName(matchingArtifact.fileName, buildDetails.number));
        }
        log.info("Downloading build artifact {} to {}", fullUrl, destinationFilePath);
        String fileData = IOUtils.read(fullUrl);
        IOUtils.write(new File(destinationFilePath), fileData);
        replacementVariables.addVariable(ReplacementVariables.VariableName.LAST_DOWNLOADED_FILE, destinationFilePath);
    }

    private JobBuildDetails getJobBuildDetails() {
        JobBuildDetails buildDetails;
        if (jenkinsConfig.hasConfiguredArtifact()) {
            log.info("Downloading artifact {} from job {} with build number {}", jenkinsConfig.jobArtifact,
                    jobWithArtifactName(), jenkinsConfig.jobBuildNumber);
            buildDetails = jenkins.getJobBuildDetails(jenkinsConfig.jobWithArtifact, jenkinsConfig.jobBuildNumber);
        } else {
            JobBuild build = determineBuildToUse();
            buildDetails = jenkins.getJobBuildDetails(build);
        }
        return buildDetails;
    }

    private JobBuild determineBuildToUse() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
        if (draft.selectedBuild != null) {
            return matchingBuilds.get(draft.selectedBuild);
        }
        if (matchingBuilds.size() == 1) {
            log.info("Using build {} as it is the only Jenkins build", matchingBuilds.get(0).buildDisplayName);
            draft.selectedBuild = 0;
            return matchingBuilds.get(0);
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
            int selection = InputUtils.readSelection(choices, "Select jenkins builds to download artifact " + jenkinsConfig.jobArtifact + " for");
            draft.selectedBuild = selection;
            return matchingBuilds.get(selection);
        }
    }
}
