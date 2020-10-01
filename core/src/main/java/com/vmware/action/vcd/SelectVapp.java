package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.ReplacementVariables;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuildDetails;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.QueryResultVappType;

@ActionDescription("Select a specific Vapp.")
public class SelectVapp extends BaseVappAction {
    public SelectVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(sshConfig.usesSshSite(), "ssh site is configured");
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (vcdConfig.useOwnedVappsOnly) {
            super.failIfTrue(vappData.getVapps().isEmpty(), "no Vapps loaded");
        } else {
            super.failIfTrue(vappData.getVapps().isEmpty() && StringUtils.isEmpty(vcdConfig.vappJsonFile) && !jenkinsConfig.hasConfiguredArtifact(),
                    "no Vapps loaded");
        }
    }

    @Override
    public void process() {
        if (!vcdConfig.useOwnedVappsOnly && StringUtils.isNotEmpty(vcdConfig.vappJsonFile)) {
            log.info("Using Vapp json file {}", vcdConfig.vappJsonFile);
            vappData.setSelectedVapp(new QueryResultVappType("url", vcdConfig.vappJsonFile));
        } else if (!vcdConfig.useOwnedVappsOnly && jenkinsConfig.hasConfiguredArtifact()) {
            JobBuildDetails buildDetails = serviceLocator.getJenkins().getJobBuildDetails(jobWithArtifactName(), jenkinsConfig.jobBuildNumber);
            String jobArtifactPath = buildDetails.fullUrlForArtifact(jenkinsConfig.jobArtifact);
            log.info("Using artifact {}", jobArtifactPath);
            vappData.setSelectedVapp(new QueryResultVappType("artifact", jobArtifactPath));
        } else if (StringUtils.isNotEmpty(vcdConfig.vappName)) {
            log.info("Using specified Vapp name {}", vcdConfig.vappName);
            vappData.setSelectedVappByName(vcdConfig.vappName);
        } else if (!vappData.noVappSelected()) {
            log.info("Using already selected Vapp {}", vappData.getSelectedVappName());
        } else {
            int selectedVapp = InputUtils.readSelection(vappData.vappLabels(),
                    "Select Vapp (Total powered on owned VM count " + vappData.poweredOnVmCount() + ")");
            vappData.setSelectedVappByIndex(selectedVapp);
        }
        if (StringUtils.isNotBlank(vappData.getSelectedVappName())) {
            String vappNameWithoutPeriods = vappData.getSelectedVappName().replace(".", "");
            replacementVariables.addVariable(ReplacementVariables.VariableName.VAPP_NAME, vappNameWithoutPeriods);
        }
    }
}
