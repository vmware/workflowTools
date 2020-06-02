package com.vmware.action.base;

import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.VappData;

public abstract class BaseVappAction extends BaseCommitAction {
    protected boolean checkVappJson;
    protected boolean checkIfSiteSelected;
    protected boolean checkIfCellSelected;
    protected VappData vappData;

    public BaseVappAction(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        failIfTrue(checkVappJson && vappData.noVappSelected(), "no Vapp selected");
        failIfTrue(checkVappJson && !vappData.jsonDataLoaded(), "no Vapp json loaded");
        failIfTrue((checkIfSiteSelected || checkIfCellSelected) && vappData.getSelectedSite() == null, "no vcd site selected");
        failIfTrue(checkIfCellSelected && vappData.getSelectedVcdCell() == null, "no vcd cell selected");
    }

    public void setVappData(VappData vappData) {
        this.vappData = vappData;
    }

}
