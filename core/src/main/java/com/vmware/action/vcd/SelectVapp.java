package com.vmware.action.vcd;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

@ActionDescription("Select a specific Vapp.")
public class SelectVapp extends BaseVappAction {
    public SelectVapp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (vappData.getOwnedVapps().isEmpty()) {
            return "no vapps loaded";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        int selectedVapp = InputUtils.readSelection(vappData.ownedVappLabels(),
                "Select Vapp (Total powered on VM count " + vappData.poweredOnVmCount() + ")");
        vappData.setSelectedVappByIndex(selectedVapp);
    }
}
