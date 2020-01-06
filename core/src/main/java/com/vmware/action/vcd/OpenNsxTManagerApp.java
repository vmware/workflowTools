package com.vmware.action.vcd;

import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;
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
        if (selectedSite.nsxManagers == null || selectedSite.nsxManagers.isEmpty()) {
            exitDueToFailureCheck("no Nsx-T managers found in Vapp");
        }
    }

    @Override
    public void process() {
        Sites.DeployedVM nsxTManager = nsxTManagerUrl();
        SystemUtils.openUrl(nsxTManager.endPointURI);
        log.info("Credentials: {}", nsxTManager.cliCredentials);
    }

    private Sites.DeployedVM nsxTManagerUrl() {
        Sites.Site selectedSite = vappData.getSelectedSite();
        if (selectedSite.nsxManagers.size() == 1) {
            log.info("Using first NSX-T Manager {} as there is only one NSX-T manager", selectedSite.nsxManagers.get(0).name);
            return selectedSite.nsxManagers.get(0);
        } else {
            List<InputListSelection> values = selectedSite.nsxManagers.stream().map(vc -> ((InputListSelection) vc)).collect(Collectors.toList());
            int selection = InputUtils.readSelection(values, "Select NSX-T Manager");
            return selectedSite.nsxManagers.get(selection);
        }
    }
}
