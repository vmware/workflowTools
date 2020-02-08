package com.vmware.action.vcd;

import java.io.File;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Select a specific Vapp.")
public class SelectVapp extends BaseVappAction {
    public SelectVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (sshConfig.usesSshSite()) {
            return "ssh site is configured";
        }
        return super.cannotRunAction();
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (vappData.getVapps().isEmpty() && StringUtils.isEmpty(vcdConfig.vappJsonFile) && !jenkinsConfig.hasConfiguredArtifact()) {
            exitDueToFailureCheck("no vapps loaded");
        }
    }

    @Override
    public void process() {
        if (StringUtils.isNotEmpty(vcdConfig.vappJsonFile)) {
            log.info("Using Vapp json file {}", vcdConfig.vappJsonFile);
            vappData.setSelectedVapp(new QueryResultVappType("url", vcdConfig.vappJsonFile));
        } else if (jenkinsConfig.hasConfiguredArtifact()) {
            String jobArtifactPath = serviceLocator.getJenkins()
                    .constructFullArtifactPath(jenkinsConfig.jobsDisplayNames[0], jenkinsConfig.jobBuildNumber, jenkinsConfig.jobArtifact);
            log.info("Using artifact {}", jobArtifactPath);
            vappData.setSelectedVapp(new QueryResultVappType("artifact", jobArtifactPath));
        } else if (StringUtils.isNotEmpty(vcdConfig.vappName)) {
            log.info("Using specified Vapp name {}", vcdConfig.vappName);
            vappData.setSelectedVappByName(vcdConfig.vappName);
        } else if (!vappData.noVappSelected()) {
            log.info("Using already selected Vapp {}", vappData.getSelectedVapp().getLabel());
        } else {
            int selectedVapp = InputUtils.readSelection(vappData.vappLabels(),
                    "Select Vapp (Total powered on owned VM count " + vappData.poweredOnVmCount() + ")");
            vappData.setSelectedVappByIndex(selectedVapp);
        }
    }
}
