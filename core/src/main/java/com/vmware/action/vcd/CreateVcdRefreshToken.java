package com.vmware.action.vcd;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

@ActionDescription("Create a VCD refresh token.")
public class CreateVcdRefreshToken extends BaseAction {
    public CreateVcdRefreshToken(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        serviceLocator.getVcd().createRefreshTokenIfNeeded();
    }
}
