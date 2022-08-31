package com.vmware.action.vcd;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected Avi Controller management page")
public class OpenAviController extends BaseSingleVappJsonAction {
    public OpenAviController(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        Sites.Site selectedSite = vappData.getSelectedSite();
        super.failIfTrue(selectedSite.aviControllers == null || selectedSite.aviControllers.isEmpty(), "no Avi Controllers found in Vapp");
    }

    @Override
    public void process() {
        Sites.DeployedVM aviController = selectDeployedVm(vappData.getSelectedSite().aviControllerVms(), "Avi Controller");
        openUiUrl(aviController);
    }
}
