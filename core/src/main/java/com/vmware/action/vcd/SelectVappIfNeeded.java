package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Select Vapp if needed for quota check or metadata.")
public class SelectVappIfNeeded extends SelectVapp {
    public SelectVappIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!jenkinsConfig.useVappJsonParameter && !vcdConfig.checkVmQuota) {
            return "checkVmQuota and useVappJsonParameter are set to false";
        } else if (jenkinsConfig.useVappJsonParameter) {
            return null;
        }
        if (vappData.getOwnedVapps().isEmpty()) {
            return "no vapps loaded";
        }
        int poweredOnVmCount = vappData.poweredOnVmCount();
        if (poweredOnVmCount <= vcdConfig.vcdVmQuota) {
            return "powered on VM count " + poweredOnVmCount + " is less than " + vcdConfig.vcdVmQuota;
        }
        return super.cannotRunAction();
    }
}
