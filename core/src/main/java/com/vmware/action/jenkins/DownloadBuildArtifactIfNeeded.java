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
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (StringUtils.isNotBlank(fileSystemConfig.fileData) && !jenkinsConfig.alwaysDownload) {
            skipActionDueTo("artifact {} has already been loaded, use flag --always-download to override and select a build to use",
                    jenkinsConfig.jobArtifact);
        }
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(fileSystemConfig.fileData)) {
            log.info("No file data loaded for artifact {}", jenkinsConfig.jobArtifact);
        }
        super.process();
    }
}
