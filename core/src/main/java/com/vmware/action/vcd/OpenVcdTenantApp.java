package com.vmware.action.vcd;

import com.google.gson.Gson;
import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.BrowserUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the tenant page for the specified Vapp and tenant")
public class OpenVcdTenantApp extends BaseSingleVappAction {

    public OpenVcdTenantApp(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String failWorkflowIfConditionNotMet() {
        if (StringUtils.isBlank(draft.vappJsonForJenkinsJob)) {
            return "no Vapp json loaded";
        }
        return super.failWorkflowIfConditionNotMet();
    }

    @Override
    public void process() {
        log.info("Selected Vapp {}", vappData.getSelectedVapp().name);

        String vcdTenant;
        if (StringUtils.isNotBlank(vcdConfig.vcdTenant)) {
            log.info("Using vcd tenant parameter {}", vcdConfig.vcdTenant);
            vcdTenant = vcdConfig.vcdTenant;
        } else {
            vcdTenant = InputUtils.readValueUntilNotBlank("Enter Vcd Tenant");
        }

        Gson gson = new ConfiguredGsonBuilder().build();
        Sites vcdSites = gson.fromJson(draft.vappJsonForJenkinsJob, Sites.class);
        String uiUrl = vcdSites.uiUrl(vcdConfig.vcdSiteIndex, vcdConfig.vcdCellIndex) + "/tenant/" + vcdTenant;
        BrowserUtils.openUrl(uiUrl);
    }
}
