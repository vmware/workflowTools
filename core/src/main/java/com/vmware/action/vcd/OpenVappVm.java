package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected Vapp Vm.")
public class OpenVappVm extends BaseSingleVappJsonAction {
    public OpenVappVm(WorkflowConfig config) {
        super(config);
        super.checkIfVmSelected = true;
    }

    @Override
    public void process() {
        Sites.VmInfo selectedVm = vappData.getSelectedVm();
        SystemUtils.openUrl(selectedVm.getUiUrl());
    }
}