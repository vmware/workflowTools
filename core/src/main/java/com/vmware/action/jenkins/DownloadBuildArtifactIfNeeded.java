package com.vmware.action.jenkins;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Downloads build artifact if an artifact is configured.")
public class DownloadBuildArtifactIfNeeded extends DownloadBuildArtifact {
    public DownloadBuildArtifactIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (!jenkinsConfig.hasConfiguredArtifact() && draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl).isEmpty()) {
            skipActionDueTo("Jenkins artifact is not configured and there are no builds in the commit testing done section");
        }
    }

    @Override
    public void process() {
        if (StringUtils.isNotBlank(fileSystemConfig.fileData) && !jenkinsConfig.alwaysDownload) {
            log.info("File data has alread been loaded, use flag --always-download to override and select a build to use");
            return;
        } else if (StringUtils.isEmpty(fileSystemConfig.fileData)) {
            log.info("No file data loaded for artifact {}", jenkinsConfig.jobArtifact);
        }
        super.process();
    }
}
