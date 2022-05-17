package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected VCenter managed object browser page")
public class OpenVCenterMob extends BaseSingleVappJsonAction {
    public OpenVCenterMob(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    public void process() {
        Sites.DeployedVM selectedVCenter = selectDeployedVm(vappData.getSelectedSite().vcVms(), "VC Server");
        SystemUtils.openUrl(selectedVCenter.endPointURI + "/mob");
        log.info("Credentials: {}", selectedVCenter.credentials);
    }
}
