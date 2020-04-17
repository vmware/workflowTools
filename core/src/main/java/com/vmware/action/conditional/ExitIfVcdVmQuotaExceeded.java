package com.vmware.action.conditional;

import com.vmware.action.base.BaseVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Exit if vm quota exceeded in Vcloud Director (includes vm count for testbeds to be deployed) and not using an existing Vapp.")
public class ExitIfVcdVmQuotaExceeded extends BaseVappAction {
    public ExitIfVcdVmQuotaExceeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(jenkinsConfig.useVappJsonParameter, "useVappJsonParameter is set to true");
    }

    @Override
    public void process() {
        int poweredOnVmCount = vappData.poweredOnVmCount();
        int testbedTemplateVmCount = vappData.getTestbedTemplateVmCount();
        int totalVmCount = poweredOnVmCount + testbedTemplateVmCount;

        if (poweredOnVmCount + testbedTemplateVmCount > vcdConfig.vcdVmQuota) {
            cancelWithMessage(String.format("Total Vm count %s (powered on count %s, template count %s) exceeds quota of %s",
                    totalVmCount, poweredOnVmCount, testbedTemplateVmCount, vcdConfig.vcdVmQuota));
        } else {
            log.info("Total Vm count {} (powered on count {}, template count {}) does not exceed quota of {}",
                    totalVmCount, poweredOnVmCount, testbedTemplateVmCount, vcdConfig.vcdVmQuota);
        }
    }
}
