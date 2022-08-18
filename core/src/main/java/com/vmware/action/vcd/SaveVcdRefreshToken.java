package com.vmware.action.vcd;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.util.input.InputUtils;

@ActionDescription("Saves a VCD refresh token.")
public class SaveVcdRefreshToken extends BaseAction {
    public SaveVcdRefreshToken(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String refreshToken = InputUtils.readValueUntilNotBlank("Refresh Token");
        serviceLocator.getVcd().saveRefreshToken(refreshToken);
    }
}
