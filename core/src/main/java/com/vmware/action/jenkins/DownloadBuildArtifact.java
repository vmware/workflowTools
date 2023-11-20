package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuildArtifact;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.FileUtils;
import com.vmware.util.IOUtils;
import com.vmware.util.MatcherUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.CancelException;
import com.vmware.util.input.InputUtils;
import com.vmware.util.logging.LogLevel;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Download a specified artifact from a jenkins build.")
public class DownloadBuildArtifact extends BaseCommitWithJenkinsBuildsAction {
    public DownloadBuildArtifact(WorkflowConfig config) {
        super(config, true);
        super.addFailWorkflowIfBlankProperties("jobArtifact");
    }

    @Override
    public void process() {
        JobBuild buildDetails = getJobBuildDetails();
        replacementVariables.addVariable(ReplacementVariables.VariableName.BUILD_NUMBER, buildDetails.number());

        JobBuildArtifact matchingArtifact = buildDetails.getArtifactForPathPattern(jenkinsConfig.jobArtifact);
        String fullUrl = buildDetails.fullUrlForArtifact(matchingArtifact);

        String downloadedFileName = FileUtils.appendToFileName(matchingArtifact.fileName, buildDetails.number());
        log.info("Downloading build artifact {}", fullUrl);
        fileSystemConfig.fileData = IOUtils.read(fullUrl);
        replacementVariables.addVariable(ReplacementVariables.VariableName.LAST_DOWNLOADED_FILE_NAME, downloadedFileName);
    }

    private JobBuild getJobBuildDetails() {
        JobBuild buildDetails;
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
        if (vappData.getSelectedVapp() != null && StringUtils.isNotBlank(vcdConfig.buildNumberInNamePattern)
                && jenkinsConfig.hasConfiguredArtifactWithoutBuildNumber()) {
            QueryResultVappType selectedVapp = vappData.getSelectedVapp();
            JobBuild matchingJobBuild = getJobBuildForName(selectedVapp.name);
            if (matchingJobBuild == null) {
                QueryResultVMsType vms = serviceLocator.getVcd().queryVmsForVapp(selectedVapp.parseIdFromRef());
                matchingJobBuild = vms.record.stream().map(vm -> getJobBuildForName(vm.name)).filter(Objects::nonNull).findFirst().orElse(null);
            }
            if (matchingJobBuild != null) {
                return matchingJobBuild;
            }
        }

        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
        if (draft.selectedBuild != null) {
            return matchingBuilds.get(draft.selectedBuild);
        }
        if (matchingBuilds.isEmpty()) {
            throw new CancelException(LogLevel.ERROR, "No matching builds found for url " + jenkinsConfig.jenkinsUrl);
        } else if (matchingBuilds.size() == 1) {
            log.info("Using build {} as it is the only Jenkins build", matchingBuilds.get(0).name);
            draft.selectedBuild = 0;
            return matchingBuilds.get(0);
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.name));
            int selection = InputUtils.readSelection(choices, "Select jenkins builds to download artifact " + jenkinsConfig.jobArtifact + " for");
            draft.selectedBuild = selection;
            return matchingBuilds.get(selection);
        }
    }

    private JobBuild getJobBuildForName(String name) {
        String buildNumber = MatcherUtils.singleMatch(name, vcdConfig.buildNumberInNamePattern);
        if (StringUtils.isInteger(buildNumber)) {
            String jobName = jobWithArtifactName();
            log.info("Found matching build {} from name {} for job {}", buildNumber, name, jobName);
            String jobUrl = UrlUtils.addRelativePaths(jenkins.baseUrl, "job", jobName);
            return new JobBuild(Integer.parseInt(buildNumber), jobUrl);
        } else {
            return null;
        }
    }
}
