package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Only loads owned Vapps for user if quota flag is set to true.")
public class LoadVappsIfNeeded extends LoadVapps {

    public LoadVappsIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (!vcdConfig.checkVmQuota && !jenkinsConfig.useVappJsonParameter) {
            return "checkVmQuota and useVappJsonParameter are set to false";
        }
        if (sshConfig.usesSshSite()) {
            return "ssh site is configured";
        }
        return super.cannotRunAction();
    }
}