package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.BrowserUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the tenant page for the specified Vapp and tenant")
public class OpenVcdTenantApp extends OpenVcdProviderApp {

    public OpenVcdTenantApp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String vcdTenant;
        if (StringUtils.isNotBlank(vcdConfig.vcdTenant)) {
            log.info("Using vcd tenant parameter {}", vcdConfig.vcdTenant);
            vcdTenant = vcdConfig.vcdTenant;
        } else {
            vcdTenant = InputUtils.readValueUntilNotBlank("Enter Vcd Tenant");
        }

        String uiUrl = uiUrl() + "/tenant/" + vcdTenant;
        BrowserUtils.openUrl(uiUrl);
    }
}
