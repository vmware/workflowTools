package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.util.CollectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.vcd.domain.VappData;

public abstract class BaseVappAction extends BaseCommitAction {
    protected boolean checkVappJson;
    protected boolean checkIfSiteSelected;
    protected boolean checkIfVmSelected;
    protected boolean checkIfCellSelected;
    protected boolean skipIfFileBasedVapp;
    protected VappData vappData;

    public BaseVappAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        failIfTrue(checkVappJson && vappData.noVappSelected(), "no Vapp selected");
        failIfTrue(checkVappJson && !vappData.jsonDataLoaded() && CollectionUtils.isEmpty(vappData.getSelectedVapp().getVcdSites()), "no Vapp json loaded");
        failIfTrue((checkIfSiteSelected || checkIfCellSelected) && vappData.getSelectedSite() == null, "no vcd site selected");
        failIfTrue(checkIfCellSelected && vappData.getSelectedVcdCell() == null, "no vcd cell selected");
        failIfTrue(checkIfVmSelected && vappData.getSelectedVm() == null, "no VM selected");
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        if (skipIfFileBasedVapp && vappData.getSelectedVapp().isJsonFileBased()) {
            skipActionDueTo("vapp is file based");
        }
    }

    public void setVappData(VappData vappData) {
        this.vappData = vappData;
    }

    protected String jobWithArtifactName() {
        if (!jenkinsConfig.hasConfiguredArtifactWithoutBuildNumber()) {
            return null;
        }
        return StringUtils.isNotEmpty(jenkinsConfig.jobWithArtifact) ? jenkinsConfig.jobWithArtifact : jenkinsConfig.jobsDisplayNames[0];
    }

}
