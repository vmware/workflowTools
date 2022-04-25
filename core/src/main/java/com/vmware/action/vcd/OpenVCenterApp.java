package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected VCenter management page")
public class OpenVCenterApp extends BaseSingleVappJsonAction {
    public OpenVCenterApp(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    public void process() {
        Sites.DeployedVM selectedVCenter = selectDeployedVm(vappData.getSelectedSite().vcServers, "VC Server");
        SystemUtils.openUrl(selectedVCenter.endPointURI + "/ui");
        log.info("Credentials: {}", selectedVCenter.credentials);
    }
}
