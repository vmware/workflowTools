package com.vmware.action.jenkins;

import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.IOUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@ActionDescription("Download build log page output")
public class DownloadBuildLog extends BaseCommitWithJenkinsBuildsAction {
    public DownloadBuildLog(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        JobBuild buildToUse = determineJobBuildToUse();
        fileSystemConfig.fileData = IOUtils.read(URI.create(buildToUse.logTextUrl()));
        replacementVariables.addVariable(ReplacementVariables.VariableName.BUILD_NUMBER, buildToUse.buildNumber());
    }

    private JobBuild determineJobBuildToUse() {
        if (StringUtils.isNotBlank(jenkinsConfig.jobWithArtifact) && jenkinsConfig.jobBuildNumber != null) {
            log.info("Downloading build log from job {} with build number {}", jobWithArtifactName(), jenkinsConfig.jobBuildNumber);
            String jobUrl = UrlUtils.addRelativePaths(jenkins.baseUrl, "job", jenkinsConfig.jobWithArtifact);
            return new JobBuild(jenkinsConfig.jobBuildNumber, jobUrl);
        } else {
            List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
            if (matchingBuilds.size() == 1) {
                log.info("Downloading build log from build {} as it is the only Jenkins build", matchingBuilds.get(0).getLabel());
                return matchingBuilds.get(0);
            } else {
                List<InputListSelection> choices = matchingBuilds.stream().map(build -> ((InputListSelection) build)).collect(Collectors.toList());
                int selection = InputUtils.readSelection(choices, "Select jenkins build to download log text from");
                return matchingBuilds.get(selection);
            }
        }
    }
}
