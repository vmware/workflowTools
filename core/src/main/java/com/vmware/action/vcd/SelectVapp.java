package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Select a specific Vapp.")
public class SelectVapp extends BaseVappAction {
    public SelectVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (StringUtils.isNotEmpty(vcdConfig.vappJsonFile)) {
            return "vappJsonFile has been specified";
        }
        return super.cannotRunAction();
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        if (vappData.getVapps().isEmpty()) {
            exitDueToFailureCheck("no vapps loaded");
        }
    }

    @Override
    public void process() {
        if (StringUtils.isNotEmpty(vcdConfig.vappName)) {
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
