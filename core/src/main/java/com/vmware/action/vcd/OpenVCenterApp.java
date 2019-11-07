package com.vmware.action.vcd;

import java.util.List;
import java.util.stream.Collectors;

import com.vmware.action.base.BaseSingleVappJsonAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.BrowserUtils;
import com.vmware.util.input.InputListSelection;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.Sites;

@ActionDescription("Opens the selected VCenter management page")
public class OpenVCenterApp extends BaseSingleVappJsonAction {
    public OpenVCenterApp(WorkflowConfig config) {
        super(config);
        super.checkIfSiteSelected = true;
    }

    @Override
    public void process() {
        String vcServerUrl = vcServerUrl();
        BrowserUtils.openUrl(vcServerUrl);
    }

    private String vcServerUrl() {
        Sites.Site selectedSite = vappData.getSelectedSite();
        if (selectedSite.vcServers.size() == 1) {
            log.info("Using first VCenter {} as there is only one VCenter", selectedSite.vcServers.get(0).name);
            return selectedSite.vcServers.get(0).endPointURI;
        } else {
            List<InputListSelection> vcValues = selectedSite.vcServers.stream().map(vc -> ((InputListSelection) vc)).collect(Collectors.toList());
            int selection = InputUtils.readSelection(vcValues, "Select VCenter");
            return selectedSite.vcServers.get(selection).endPointURI;
        }
    }
}
