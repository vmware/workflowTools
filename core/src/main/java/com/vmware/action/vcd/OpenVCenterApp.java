package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.chrome.ChromeDevTools;
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
        Sites.DeployedVM selectedVCenter = selectDeployedVm(vappData.getSelectedSite().vcVms(), "VC Server");
        openUiUrl(selectedVCenter);
    }
}
