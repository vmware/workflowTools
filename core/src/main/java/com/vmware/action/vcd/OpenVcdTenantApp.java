package com.vmware.action.vcd;

import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the tenant page for the specified Vapp and tenant")
public class OpenVcdTenantApp extends OpenVcdProviderApp {

    public OpenVcdTenantApp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String vcdTenant;
        if (StringUtils.isNotEmpty(vcdConfig.defaultVcdOrg)) {
            log.info("Using vcd tenant parameter {}", vcdConfig.defaultVcdOrg);
            vcdTenant = vcdConfig.defaultVcdOrg;
        } else {
            vcdTenant = InputUtils.readValueUntilNotBlank("Enter Vcd Tenant");
        }

        String uiUrl = UrlUtils.addRelativePaths(uiCell().endPointURI, "tenant", vcdTenant);
        SystemUtils.openUrl(uiUrl);
    }
}
