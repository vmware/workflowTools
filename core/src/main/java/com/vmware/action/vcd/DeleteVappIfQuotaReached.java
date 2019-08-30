package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Delete selected Vapp from Vcloud Director if quota reached and not using an existing Vapp.")
public class DeleteVappIfQuotaReached extends DeleteVapp {
    public DeleteVappIfQuotaReached(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (jenkinsConfig.useVappJsonParameter) {
            return "useVappJsonParameter is set to true";
        }
        int poweredOnVmCount = vappData.poweredOnVmCount();
        if (poweredOnVmCount <= vcdConfig.vcdVmQuota) {
            return "powered on VM count " + poweredOnVmCount + " is less than " + vcdConfig.vcdVmQuota;
        }
        return super.cannotRunAction();
    }
}
