package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.chrome.ChromeDevTools;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected Nsx-T manager management page")
public class OpenNsxTManagerApp extends BaseSingleVappJsonAction {
    public OpenNsxTManagerApp(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        Sites.Site selectedSite = vappData.getSelectedSite();
        super.failIfTrue(selectedSite.nsxTManagerVms().isEmpty(), "no Nsx-T managers found in Vapp");
    }

    @Override
    public void process() {
        Sites.DeployedVM nsxTManager = selectDeployedVm(vappData.getSelectedSite().nsxTManagerVms(), "Nsx-T Manager");
        openUiUrl(nsxTManager);
    }
}
