package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Selects a VCD cell.")
public class SelectVcdCell extends BaseSingleVappJsonAction {
    public SelectVcdCell(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        selectVcdCell(vappData.getSelectedSite(), vcdConfig.vcdCellIndex);
    }
}
