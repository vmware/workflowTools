package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Loads Json metadata for selected Vapp if needed")
public class LoadVappJsonIfNeeded extends LoadVappJson {
    public LoadVappJsonIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(!jenkinsConfig.useVappJsonParameter, "useVappJsonParameter is set to false");
    }
}
