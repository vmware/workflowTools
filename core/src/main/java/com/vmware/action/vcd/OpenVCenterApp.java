package com.vmware.action.vcd;

import com.google.gson.Gson;
import com.vmware.action.base.BaseSingleVappAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.util.BrowserUtils;
import com.vmware.util.StringUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected VCenter management page")
public class OpenVCenterApp extends BaseSingleVappAction {
    public OpenVCenterApp(WorkflowConfig config) {
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
        Gson gson = new ConfiguredGsonBuilder().build();
        Sites vcdSites = gson.fromJson(draft.vappJsonForJenkinsJob, Sites.class);
        String vcServerUrl = vcdSites.vcServerUrl(vcdConfig.vcdSiteIndex);
        BrowserUtils.openUrl(vcServerUrl);
    }
}
