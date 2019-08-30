package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Loads Json metadata for selected Vapp if needed")
public class LoadVappJsonIfNeeded extends LoadVappJson {
    public LoadVappJsonIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!jenkinsConfig.useVappJsonParameter) {
            return "useVappJsonParameter is set to false";
        }
        return super.cannotRunAction();
    }
}
