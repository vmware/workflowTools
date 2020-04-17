package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Only loads owned Vapps for user if quota flag is set to true.")
public class LoadVappsIfNeeded extends LoadVapps {

    public LoadVappsIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void checkIfActionShouldBeSkipped() {
        super.checkIfActionShouldBeSkipped();
        super.skipActionIfTrue(!vcdConfig.checkVmQuota && !jenkinsConfig.useVappJsonParameter,
                "checkVmQuota and useVappJsonParameter are set to false");
        super.skipActionIfTrue(sshConfig.usesSshSite(), "ssh site is configured");
    }
}